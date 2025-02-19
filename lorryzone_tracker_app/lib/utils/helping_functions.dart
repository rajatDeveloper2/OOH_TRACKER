//function to handle the responvise height

import 'dart:math';


import 'package:flutter/material.dart';
import 'package:lorryzone_tracker_app/res/colors.dart';
import 'package:shared_preferences/shared_preferences.dart';

double getDeviceHeight(BuildContext context) =>
    MediaQuery.of(context).size.height;

//function to handle the responvise width
double getDeviceWidth(BuildContext context) =>
    MediaQuery.of(context).size.width;

//using custom snack bar
void showSnackBar(BuildContext context, String text) {
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(
      content: Text(
        text,
        style: const TextStyle(color: AppColors.blueColor),
      ),
      backgroundColor: Colors.white,
    ),
  );
}

//using this function for responsive text
double getFontSize(double size, double screenWidth) {
  return size * screenWidth / 414;
}

//shared preferences functions
Future<void> setDataToLocal(
    {required String key, required String value}) async {
  SharedPreferences prefs = await SharedPreferences.getInstance();
  await prefs.setString(key, value);
}

//function to get the saved data using key from local
Future<String> getSavedDataByKey({required String key}) async {
  SharedPreferences prefs = await SharedPreferences.getInstance();

  String? data = prefs.getString(key);
  return data ?? "";
}

//remove the local saved data using key
Future<void> removeDataByKey({required String key}) async {
  SharedPreferences prefs = await SharedPreferences.getInstance();
  await prefs.remove(key);
}

// Future<File?> pickImageAndReturnImg({required ImageSource imageType}) async {
//   try {
//     final pickedImage =
//         await ImagePicker().pickImage(source: imageType, imageQuality: 50);

//     return File(pickedImage!.path);
//   } catch (e) {
//     return null;
//   }
// }

String generateRandomNumberString(int length) {
  final random = Random();
  String randomNumber = '';

  // Generate a random number string of the specified length
  for (int i = 0; i < length; i++) {
    randomNumber +=
        random.nextInt(10).toString(); // Generate random digits (0-9)
  }

  return randomNumber;
}

// Future<File> writeBytesToFile(Uint8List bytes, String filename) async {
//   // Get the temporary directory of the app
//   final directory = await getTemporaryDirectory();

//   // Create the file path
//   final filePath = '${directory.path}/$filename';

//   // Create the file
//   final file = File(filePath);

//   // Write the Uint8List to the file
//   await file.writeAsBytes(bytes);

//   return file;
// }
