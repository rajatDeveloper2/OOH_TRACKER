// ignore_for_file: use_build_context_synchronously

import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:lorryzone_tracker_app/core/key/user_key.dart';
import 'package:lorryzone_tracker_app/feature/auth/view/login_view.dart';
import 'package:lorryzone_tracker_app/feature/home/view/home_view.dart';
import 'package:lorryzone_tracker_app/provider/main_provider.dart';
import 'package:lorryzone_tracker_app/utils/helping_functions.dart';
import 'package:provider/provider.dart';

class AuthController {
  //Login Controller

  TextEditingController phoneNumberController = TextEditingController();

  TextEditingController otpController = TextEditingController();
  // Foucs Node
  FocusNode phoneFoucseNode = FocusNode();
  FocusNode otpFoucseNode = FocusNode();

  //Navigate to home or login
  navigateHomeOrLogin({required BuildContext context}) async {
    String tokenFromLocal = await getSavedDataByKey(key: KeyData.tokenKey);

    // left work get local value or not set default values

    log("User Token: $tokenFromLocal");
    if (tokenFromLocal == "") {
      await Future.delayed(Duration(seconds: 1));
      Navigator.pushNamedAndRemoveUntil(
        context,
        LoginView.tag,
        (Route<dynamic> route) => false, // Removes all previous routes
      );
    } else {
      var provider = context.read<MainProvider>();
      await Future.delayed(Duration(seconds: 1));
      provider.token = tokenFromLocal;

      Navigator.pushNamedAndRemoveUntil(
        context,
        HomeView.tag,
        (Route<dynamic> route) => false, // Removes all previous routes
      );
      // API is not working
      // get User Data And Verify the token
      // provider.verifyTheTokenAndGetUserData(context: context);
    }
  }
}
