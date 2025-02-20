import 'package:flutter/material.dart';
import 'package:intl_phone_field/intl_phone_field.dart';
import 'package:lorryzone_tracker_app/core/widgets/custom_button.dart';
import 'package:lorryzone_tracker_app/feature/auth/controller/auth_controller.dart';
import 'package:lorryzone_tracker_app/feature/auth/view/otp_screen.dart';
import 'package:lorryzone_tracker_app/provider/main_provider.dart';
import 'package:lorryzone_tracker_app/res/colors.dart';
import 'package:lorryzone_tracker_app/utils/helping_functions.dart';
import 'package:provider/provider.dart';

class LoginView extends StatefulWidget {
  static const String tag = "login-view";
  const LoginView({super.key});

  @override
  State<LoginView> createState() => _LoginViewState();
}

class _LoginViewState extends State<LoginView> {
  final _authController = AuthController();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.darkGreyColor,
      appBar: AppBar(
        backgroundColor: AppColors.blackColor,
        centerTitle: true,
        title: Text(
          "OOH Tracker",
          style: TextStyle(color: AppColors.whiteColor),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.all(18.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.start,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SizedBox(height: getDeviceHeight(context) * 0.02),
                Text(
                  "Enter your phone for register/login",
                  style: TextStyle(
                    color: AppColors.whiteColor,
                    fontSize: getFontSize(15, getDeviceWidth(context)),
                  ),
                ),
                SizedBox(height: getDeviceHeight(context) * 0.02),
                IntlPhoneField(
                  focusNode: _authController.phoneFoucseNode,
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                  decoration: InputDecoration(
                    counterText: '',
                    // label: Text(
                    //   "Phone no.",
                    //   style: TextStyle(color: AppColors.whiteColor),
                    // ),
                    hintText: "XXXXXXXXXX",
                    enabledBorder: const OutlineInputBorder(
                      borderRadius: BorderRadius.all(Radius.circular(10)),
                      borderSide: BorderSide(
                        color:
                            AppColors
                                .blackColor, // Use a default border color for inactive state
                      ),
                    ),
                    disabledBorder: const OutlineInputBorder(
                      borderRadius: BorderRadius.all(Radius.circular(10)),
                      borderSide: BorderSide(
                        color:
                            AppColors
                                .darkGreyColor, // Use a default border color for disabled state
                      ),
                    ),
                    focusedBorder: const OutlineInputBorder(
                      borderRadius: BorderRadius.all(Radius.circular(10)),
                      borderSide: BorderSide(color: AppColors.darkGreyColor),
                    ),
                    filled: true,
                    fillColor: AppColors.whiteColor,
                    border: const OutlineInputBorder(borderSide: BorderSide()),
                  ),
                  initialCountryCode: 'IN',
                  onChanged: (phone) {
                    _authController.phoneNumberController.text = phone.number;
                  },
                ),
                SizedBox(height: getDeviceHeight(context) * 0.04),

                CustomElevatedBtn(
                  text: "Send OTP",
                  onPressed: () {
                    if (_authController.phoneNumberController.text.length >=
                        10) {
                      var provider = context.read<MainProvider>();
                      String otp = generateRandomNumberString(4);
                      provider.sendOTPTo2Factor(
                        context: context,
                        mobileNumber:
                            _authController.phoneNumberController.text.trim(),
                        otp: otp,
                      );
                    } else {
                      showSnackBar(
                        context,
                        "Please enter 10 digit mobile number",
                      );
                    }
                  },
                  bgColor: AppColors.blackColor,
                  borderColor: Colors.transparent,
                  textColor: AppColors.whiteColor,
                ),
                SizedBox(height: getDeviceHeight(context) * 0.04),

                Center(
                  child: Padding(
                    padding: const EdgeInsets.all(8.0),
                    child: Text(
                      "Note: By tapping Send OTP you agree to our terms and conditions on www.OOHTracker.com",
                      style: TextStyle(
                        color: AppColors.whiteColor,
                        fontSize: getFontSize(15, getDeviceWidth(context)),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
