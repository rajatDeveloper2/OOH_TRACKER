// ignore_for_file: public_member_api_docs, sort_constructors_first, use_build_context_synchronously
import 'dart:developer';
import 'dart:io';

import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/material.dart';
import 'package:lorryzone_tracker_app/core/key/user_key.dart';
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

  Future<String?> getDeviceId() async {
    DeviceInfoPlugin deviceInfo = DeviceInfoPlugin();

    if (Platform.isAndroid) {
      AndroidDeviceInfo androidInfo = await deviceInfo.androidInfo;
      return androidInfo.id; // Unique ID for Android
    } else if (Platform.isIOS) {
      IosDeviceInfo iosInfo = await deviceInfo.iosInfo;
      return iosInfo.identifierForVendor; // Unique ID for iOS
    }

    return null;
  }

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
                  onPressed: () async {
                    if (_authController.otpController.text.length >= 4) {
                      if (widget.realOTP ==
                          _authController.otpController.text.trim()) {
                        var provider = context.read<MainProvider>();

                        String uuid = await getSavedDataByKey(
                          key: KeyData.uuid,
                        );

                        if (uuid == "") {
                          // no uuid found in local

                          //get deviceId

                          String? deviceId = null;
                          (await getDeviceId())
                              ?.replaceAll(".", "")
                              .replaceAll("-", "");

                          log("Device Id: ${deviceId}");

                          if (deviceId == null) {
                            String uuidFromApp = Uuid()
                                .v1()
                                .replaceAll(".", "")
                                .replaceAll("-", "");
                            log("New UUId from UUID: ${uuidFromApp}");
                            setDataToLocal(
                              key: KeyData.uuid,
                              value: uuidFromApp,
                            );
                            // if device id is not able to get

                            setDataToLocal(
                              key: KeyData.phoneNo,
                              value: widget.mobileNumber.replaceAll("+", ""),
                            );

                            provider.registrationFunction(
                              context: context,
                              mobileNumber: widget.mobileNumber.replaceAll(
                                "+",
                                "",
                              ),
                              otp: widget.realOTP,
                              uuid: uuidFromApp,
                            );
                            return;
                          } else {
                            // if we have deviceId
                            setDataToLocal(key: KeyData.uuid, value: deviceId);
                            setDataToLocal(
                              key: KeyData.phoneNo,
                              value: widget.mobileNumber.replaceAll("+", ""),
                            );
                            provider.registrationFunction(
                              context: context,
                              mobileNumber: widget.mobileNumber.replaceAll(
                                "+",
                                "",
                              ),
                              otp: widget.realOTP,
                              uuid: deviceId,
                            );
                            return;
                          }
                        } else {
                          // if we have uuid in local
                          // save mobile no.
                          setDataToLocal(
                            key: KeyData.phoneNo,
                            value: widget.mobileNumber.replaceAll("+", ""),
                          );
                          provider.registrationFunction(
                            context: context,
                            mobileNumber: widget.mobileNumber.replaceAll(
                              "+",
                              "",
                            ),
                            otp: widget.realOTP,
                            uuid: uuid,
                          );
                          return;
                        }
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
