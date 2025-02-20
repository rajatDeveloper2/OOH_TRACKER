// ignore_for_file: use_build_context_synchronously

import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:lorryzone_tracker_app/core/key/user_key.dart';
import 'package:lorryzone_tracker_app/core/widgets/loader.dart';
import 'package:lorryzone_tracker_app/feature/auth/model/user_model.dart';
import 'package:lorryzone_tracker_app/feature/auth/view/otp_screen.dart';
import 'package:lorryzone_tracker_app/feature/auth/view/splash_view.dart';
import 'package:lorryzone_tracker_app/feature/home/view/home_view.dart';
import 'package:lorryzone_tracker_app/services/auth_service.dart';
import 'package:lorryzone_tracker_app/utils/helping_functions.dart';

class MainProvider extends ChangeNotifier {
  final _authService = AuthService();

  String? token;
  UserModel? userModel;

  // Send OTP to 2 Factor

  sendOTPTo2Factor({
    required BuildContext context,
    required String mobileNumber,
    required String otp,
  }) async {
    try {
      showLoading(context);
      var res = await _authService.sendOTP2FactorService(
        mobileNumber: mobileNumber,
        otp: otp,
      );

      res.handleResponse(
        onFailed: () {
          hideLoading(context);
          showSnackBar(context, "Network issue, try again!");
        },
        onSuccess: () async {
          await Future.delayed(Duration(seconds: 1));
          hideLoading(context);

          Navigator.pushNamed(
            context,
            OtpScreen.tag,
            arguments: {'mobileNumber': mobileNumber, 'realOTP': otp},
          );
        },
      );
    } catch (e) {
      hideLoading(context);
      showSnackBar(context, e.toString());
    }
  }

  // verfiy the token

  verifyTheTokenAndGetUserData({required BuildContext context}) async {
    try {
      var res = await _authService.verifyUserTokenAndGetUserData(
        token: token ?? " ",
      );
      res.handleResponse(
        onFailed: () {
          showSnackBar(
            context,
            res.error?.errorMsg ?? "Please retry in some time",
          );

          removeDataByKey(key: KeyData.tokenKey);

          // Navigate to Splash Screen

          Navigator.pushNamedAndRemoveUntil(
            context,
            SplashView.tag,
            (Route<dynamic> route) => false, // Removes all previous routes
          );
        },
        onSuccess: () {
          userModel = res.data;
          notifyListeners();

          //Navigate to Home Screen
          Navigator.pushNamedAndRemoveUntil(
            context,
            HomeView.tag,
            (Route<dynamic> route) => false, // Removes all previous routes
          );
        },
      );
    } catch (e) {
      showSnackBar(context, e.toString());
    }
  }

  // genrate Token

  genrateToken({
    required BuildContext context,
    required String mobileNumber,
    required String otp,
    required String uuid,
  }) async {
    try {
      var res = await _authService.genrateTokenService(
        mobileNumber: mobileNumber,
        otp: otp,
        uuid: uuid,
      );
      res.handleResponse(
        onFailed: () {},
        onSuccess: () async {
          //Save the token to provider and local storage
          //Save to provider
          token = res.data['token'];

          //Save to local
          await setDataToLocal(key: KeyData.tokenKey, value: res.data['token']);

          // Navigate to Home
          Navigator.pushNamedAndRemoveUntil(
            context,
            HomeView.tag,
            (Route<dynamic> route) => false, // Removes all previous routes
          );
        },
      );
    } catch (e) {
      showSnackBar(context, e.toString());
    }
  }

  //registration
  registrationFunction({
    required BuildContext context,
    required String mobileNumber,
    required String otp,
    required String uuid,
  }) async {
    try {
      showLoading(context);
      var res = await _authService.registrationService(
        mobileNumber: mobileNumber,
        otp: otp,
        uuid: uuid,
      );
      res.handleResponse(
        onFailed: () {
          hideLoading(context);
        },
        onSuccess: () async {
          // hideLoading(context);
          showSnackBar(context, "SercueOtp: ${res.data['secureOTP']}");
          log("SecureOTP:${res.data['secureOTP']}");

          // genrate the Token
          genrateToken(
            context: context,
            mobileNumber: mobileNumber,
            otp: res.data['secureOTP'],
            uuid: uuid,
          );
        },
      );
    } catch (e) {
      showSnackBar(context, e.toString());
    }
  }
}
