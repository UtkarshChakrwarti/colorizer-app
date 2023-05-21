package com.example.colorizer;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.colorizer.APIservice.ApiService;
import com.example.colorizer.APIservice.ApiService.UploadResponse;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_IMAGE = 1;
    private static final int REQUEST_CODE_CAMERA = 2;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final String UPLOAD_URL = "https://color-tool.azurewebsites.net/";
    private static final String DEFAULT_IMG_URL = "https://www.reaconverter.com/howto/wp-content/uploads/2017/02/animation-1.gif";
    private static final int REQUEST_CODE_PERMISSIONS = 1001 ;

    private ImageView imageView;
    private Button galleryButton;
    private Button cameraButton;
    private ProgressBar progressBar;
    private Button saveButton;
    private Button shareButton;

    private String currentImagePath;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        galleryButton = findViewById(R.id.galleryButton);
        cameraButton = findViewById(R.id.cameraButton);
        progressBar = findViewById(R.id.progressBar);
        saveButton = findViewById(R.id.saveButton);
        shareButton = findViewById(R.id.shareButton);

        galleryButton.setOnClickListener(view -> pickImageFromGallery());
        cameraButton.setOnClickListener(view -> captureImageFromCamera());

        saveButton.setOnClickListener(view -> saveImageToGallery());
        shareButton.setOnClickListener(view -> shareImage());

        apiService = new ApiService(); // Initialize the ApiService

        // Load image using Glide and display it in the ImageView
        String imageUrl = DEFAULT_IMG_URL; // Replace with the actual image URL
        Glide.with(this)
                .load(imageUrl)
                .apply(new RequestOptions().placeholder(R.drawable.placeholder_image))
                .into(imageView);

        if (!allPermissionsGranted()) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }
    //remove all cached images and load the default image when the app is closed and reopened
    @Override
    protected void onResume() {
        super.onResume();
        Glide.get(this).clearMemory();
        resetImageView();
    }
    private void pickImageFromGallery() {
        resetImageView();
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    private void captureImageFromCamera() {
        resetImageView();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "image.jpg");

        Uri photoUri = FileProvider.getUriForFile(this, "com.example.colorizer.fileprovider", file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_PICK_IMAGE && data != null) {
                Uri imageUri = data.getData();
                currentImagePath = getRealPathFromURI(imageUri);
                loadImageFromFile(currentImagePath);
                colorizeImage();
            } else if (requestCode == REQUEST_CODE_CAMERA) {
                File imageFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "image.jpg");
                currentImagePath = imageFile.getAbsolutePath();
                loadImageFromFile(currentImagePath); // Load the captured image


                colorizeImage();
            }
        } else {
            Toast.makeText(this, "First select image to continue", Toast.LENGTH_SHORT).show();
        }
    }



    private void loadImageFromFile(String imagePath) {
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            Glide.with(this).load(imageFile).into(imageView);
        }
    }
    //reset the image view after gallery or camera button is clicked
    private void resetImageView() {
        Glide.with(this)
                .load(DEFAULT_IMG_URL)
                .apply(new RequestOptions().placeholder(R.drawable.placeholder_image))
                .into(imageView);
    }
    private void colorizeImage() {
        File imageFile = new File(currentImagePath);
        if (imageFile.exists()) {
            progressBar.setVisibility(View.VISIBLE);
            ColorizeImageTask colorizeImageTask = new ColorizeImageTask();
            colorizeImageTask.execute(imageFile);
        }
    }

    private class ColorizeImageTask extends AsyncTask<File, Void, UploadResponse> {
        private OkHttpClient client;
        private Gson gson;

        public ColorizeImageTask() {
            client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS) // Set a longer connect timeout
                    .readTimeout(60, TimeUnit.SECONDS) // Set a longer read timeout
                    .build();
            gson = new Gson();
        }

        @Override
        protected UploadResponse doInBackground(File... files) {
            File imageFile = files[0];
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", imageFile.getName(),
                            RequestBody.create(MediaType.parse("image/*"), imageFile))
                    .build();

            Request request = new Request.Builder()
                    .url(UPLOAD_URL)
                    .post(requestBody)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    return gson.fromJson(responseBody, UploadResponse.class);
                } else {
                    throw new IOException("Unexpected response code: " + response.code());
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(UploadResponse response) {
            progressBar.setVisibility(View.GONE);
            if (response != null) {
                String imageUrl = response.getImageUrl();
                if (imageUrl != null) {
                    Glide.with(MainActivity.this)
                            .load(UPLOAD_URL + imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(imageView);
                }
            } else {
                Toast.makeText(MainActivity.this, "Failed to colorize image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImageToGallery() {
        Bitmap imageBitmap = getBitmapFromImageView();
        if (imageBitmap != null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri != null) {
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(imageUri);
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.close();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
                    });
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                    });
                    e.printStackTrace();
                }
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    private void shareImage() {
        Bitmap imageBitmap = getBitmapFromImageView();
        if (imageBitmap != null) {
            File imageFile = new File(saveImageToFile(imageBitmap));
            if (imageFile.exists()) {
                Uri imageUri = FileProvider.getUriForFile(this, "com.example.colorizer.fileprovider", imageFile);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Share Image"));
            } else {
                Toast.makeText(this, "Failed to share image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap getBitmapFromImageView() {
        imageView.setDrawingCacheEnabled(true);
        imageView.buildDrawingCache();
        Bitmap bitmap = imageView.getDrawingCache().copy(Bitmap.Config.ARGB_8888, false);
        imageView.destroyDrawingCache();
        return bitmap;
    }

    private String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            return filePath;
        }
        return null;
    }

    private String saveImageToFile(Bitmap imageBitmap) {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = null;
        try {
            imageFile = File.createTempFile("image", ".jpg", storageDir);
            OutputStream out = new FileOutputStream(imageFile);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.e("MainActivity", "Failed to save image", e);
        }

        if (imageFile != null) {
            return imageFile.getAbsolutePath();
        }

        return null;
    }
}
