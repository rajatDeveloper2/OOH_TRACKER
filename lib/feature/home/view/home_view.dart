import 'dart:io';

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:lorryzone_tracker_app/res/colors.dart';
import 'package:lorryzone_tracker_app/utils/helping_functions.dart';
import 'package:path_provider/path_provider.dart';

class HomeView extends StatefulWidget {
  static const String tag = "home-view";
  const HomeView({super.key});

  @override
  State<HomeView> createState() => _HomeViewState();
}

class _HomeViewState extends State<HomeView> {
  CameraController? _controller;
  List<CameraDescription>? _cameras;
  bool _isCameraInitialized = false;
  bool _isFlashOn = false;
  int _selectedCameraIndex = 0;

  @override
  void initState() {
    super.initState();
    _initializeCamera();
  }

  Future<void> _initializeCamera() async {
    _cameras = await availableCameras();
    if (_cameras!.isNotEmpty) {
      _setCamera(_selectedCameraIndex);
    }
  }

  Future<void> _setCamera(int cameraIndex) async {
    _controller = CameraController(
      _cameras![cameraIndex],
      ResolutionPreset.medium,
    );

    await _controller!.initialize();
    setState(() {
      _isCameraInitialized = true;
    });
  }

  void _switchCamera() {
    if (_cameras != null && _cameras!.length > 1) {
      _selectedCameraIndex = (_selectedCameraIndex == 0) ? 1 : 0;
      _setCamera(_selectedCameraIndex);
    }
  }

  Future<File?> captureImage(CameraController controller) async {
    if (!controller.value.isInitialized) {
      print("Camera not initialized");
      return null;
    }

    try {
      // Capture the image
      XFile image = await controller.takePicture();

      // Get app's temporary directory
      Directory tempDir = await getTemporaryDirectory();
      String imagePath =
          "${tempDir.path}/${generateRandomNumberString(15)}_${DateTime.now()}.jpg";

      // Convert XFile to File and save it
      File imageFile = File(imagePath);
      await image.saveTo(imageFile.path);

      print("Image saved at: $imagePath");
      return imageFile;
    } catch (e) {
      print("Error capturing image: $e");
      return null;
    }
  }

  void _toggleFlash() async {
    if (_controller != null && _controller!.value.isInitialized) {
      _isFlashOn = !_isFlashOn;
      await _controller!.setFlashMode(
        _isFlashOn ? FlashMode.torch : FlashMode.off,
      );
      setState(() {});
    }
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.darkGreyColor,
      appBar: AppBar(
        backgroundColor: AppColors.blackColor,
        actions: [
          IconButton(
            icon: Icon(
              _isFlashOn ? Icons.flash_on : Icons.flash_off,
              color: Colors.white,
              size: 30,
            ),
            onPressed: _toggleFlash,
          ),
          IconButton(
            icon: Icon(Icons.settings, color: Colors.white, size: 30),
            onPressed: () {
              showSnackBar(context, "Under Development");
            },
          ),
        ],
      ),
      body: Column(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,

        children: [
          Stack(
            children: [
              // Camera Preview or Loading Indicator
              _isCameraInitialized
                  ? SizedBox(
                    height: getDeviceHeight(context) * 0.75,
                    child: CameraPreview(_controller!),
                  )
                  : Center(child: CircularProgressIndicator()),

              // Overlay Container
              Positioned(
                top: 100,
                left: 50,
                right: 50,
                child: Container(
                  width: 200,
                  height: 100,
                  color: Colors.black.withOpacity(0.5),
                  child: Center(
                    child: Text(
                      "Overlay",
                      style: TextStyle(color: Colors.white, fontSize: 20),
                    ),
                  ),
                ),
              ),
            ],
          ),
          Container(
            width: getDeviceWidth(context),
            // height: getDeviceHeight(context) * 0.2,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                IconButton(
                  onPressed: () {
                    captureImage(_controller!);
                  },
                  icon: Icon(
                    Icons.camera_alt,
                    size: getFontSize(38, getDeviceWidth(context)),
                    color: AppColors.whiteColor,
                  ),
                ),
                IconButton(
                  onPressed: _switchCamera,
                  icon: Icon(
                    Icons.cameraswitch,
                    size: getFontSize(38, getDeviceWidth(context)),
                    color: AppColors.whiteColor,
                  ),
                ),
              ],
            ),
          ),
          SizedBox(),
        ],
      ),
    );
  }
}
