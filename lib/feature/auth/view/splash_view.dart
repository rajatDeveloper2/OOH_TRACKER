import 'package:flutter/material.dart';
import 'package:lorryzone_tracker_app/feature/auth/controller/auth_controller.dart';
import 'package:lorryzone_tracker_app/res/assets.dart';
import 'package:lorryzone_tracker_app/res/colors.dart';
import 'package:lorryzone_tracker_app/utils/helping_functions.dart';

class SplashView extends StatefulWidget {
  static const String tag = "splash-view";

  const SplashView({super.key});

  @override
  State<SplashView> createState() => _SplashViewState();
}

class _SplashViewState extends State<SplashView> {
  final _authController = AuthController();
  @override
  void initState() {
    super.initState();
    _authController.navigateHomeOrLogin(context: context);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.darkGreyColor,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Image.asset(
              AssetsData.splashScreenLogo,
              height: getDeviceHeight(context) * 0.3,
            ),
            // Text(
            //   "OOH Tracker",
            //   style: TextStyle(
            //     color: AppColors.whiteColor,
            //     fontSize: getFontSize(30, getDeviceWidth(context)),
            //   ),
            // ),
          ],
        ),
      ),
    );
  }
}
