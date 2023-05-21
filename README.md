# Colorizer App

Colorizer is an Android application that allows users to colorize images either from their device's gallery or by capturing a new photo using the camera. The app utilizes an API service to process and colorize the selected images. Users can also save the colorized images to their device's gallery and share them with others.

## Features

- Choose an image from the gallery or capture a new photo using the camera.
- Colorize the selected image using the API service.
- Save the colorized image to the device's gallery.
- Share the colorized image with others.
- Reset the image view to the default image when reopening the app.
- Cache management to optimize image loading.

## Screenshots

Include some screenshots of the app here to showcase its interface and functionality.

## Installation

To use the Colorizer app, follow these steps:

1. Clone the repository: `git clone <repository-url>`
2. Open the project in Android Studio.
3. Build and run the app on an Android device or emulator.

## Usage

1. Launch the Colorizer app on your Android device.
2. Click on the "Gallery" button to choose an image from your device's gallery or click on the "Camera" button to capture a new photo.
3. Once an image is selected or captured, it will be displayed in the app's image view.
4. The app will automatically colorize the image using the API service. You can view the progress of the colorization process in the progress bar.
5. After the colorization is complete, the colorized image will replace the original image in the image view.
6. To save the colorized image to your device's gallery, click on the "Save" button.
7. To share the colorized image with others, click on the "Share" button.
8. If you close the app and reopen it, the default image will be loaded, and the cache will be cleared.

## API Service

The Colorizer app uses an API service for image colorization. The API endpoint is "https://color-tool.azurewebsites.net/". The colorization process is performed asynchronously using an AsyncTask.

## Libraries Used

- Glide: For image loading and caching.
- OkHttp: For making HTTP requests to the API service.
- Gson: For parsing JSON responses from the API service.

## License

This project is licensed under the [MIT License](LICENSE).

## Acknowledgements

Special thanks to the developers of the Glide, OkHttp, and Gson libraries for their contributions to this project.

## Contributing

Contributions to the Colorizer app are welcome! If you find any issues or have suggestions for improvements, please open an issue or submit a pull request.

## Contact

If you have any questions or inquiries, feel free to contact us at utkarshchakrwarti007@gmail.com

Happy Colorizing!
