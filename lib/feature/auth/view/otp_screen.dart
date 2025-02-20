// ignore_for_file: public_member_api_docs, sort_constructors_first
import 'package:flutter/material.dart';
import 'package:lorryzone_tracker_app/core/widgets/custom_button.dart';
import 'package:lorryzone_tracker_app/core/widgets/custom_textfield.dart';
import 'package:lorryzone_tracker_app/feature/auth/controller/auth_controller.dart';
import 'package:lorryzone_tracker_app/provider/main_provider.dart';
import 'package:lorryzone_tracker_app/res/colors.dart';
import 'package:lorryzone_tracker_app/utils/helping_functions.dart';
import 'package:provider/provider.dart';
import 'package:uuid/uuid.dart';

class OtpScreen extends StatefulWidget {
  static const String tag = "otp_screen";
  final String mobileNumber;
  final String realOTP;
  const OtpScreen({
    super.key,
    required this.mobileNumber,
    required this.realOTP,
  });

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  final _authController = AuthController();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColors.blackColor,
        centerTitle: true,
        title: Text("Enter OTP", style: TextStyle(color: AppColors.whiteColor)),
        iconTheme: IconThemeData(color: AppColors.whiteColor),
      ),
      backgroundColor: AppColors.darkGreyColor,
      body: SafeArea(
        child: SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.all(18.0),
            child: Column(
              children: [
                SizedBox(height: getDeviceHeight(context) * 0.02),
                Text(
                  "We've sent a verification code to ${widget.mobileNumber} Please enter the code below to continue.",
                  style: TextStyle(
                    color: AppColors.whiteColor,
                    fontSize: getFontSize(15, getDeviceWidth(context)),
                  ),
                ),
                SizedBox(height: getDeviceHeight(context) * 0.06),
                CustomTextField(
                  borderActiveColor: AppColors.blackColor,
                  maxLength: 4,
                  labelText: "",
                  isNum: true,
                  hintText: "Enter OTP",
                  activeTextColor: AppColors.blackColor,
                  backgroundColor: AppColors.whiteColor,
                  controller: _authController.otpController,
                  node: _authController.otpFoucseNode,
                  textColor: AppColors.blackColor,
                  textFieldTextColor: AppColors.blackColor,
                ),
                SizedBox(height: getDeviceHeight(context) * 0.06),
                CustomElevatedBtn(
                  text: "Verify OTP",
                  onPressed: () {
                    if (_authController.otpController.text.length >= 4) {
                      if (widget.realOTP ==
                          _authController.otpController.text.trim()) {
                        var provider = context.read<MainProvider>();
                        provider.registrationFunction(
                          context: context,
                          mobileNumber: widget.mobileNumber,
                          otp: widget.realOTP,
                          uuid: Uuid().v1(),
                        );
                      } else {
                        showSnackBar(context, "Please enter correct OTP");
                      }
                    } else {
                      showSnackBar(context, "Please enter OTP");
                    }
                  },
                  bgColor: AppColors.blackColor,
                  borderColor: Colors.transparent,
                  textColor: AppColors.whiteColor,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
