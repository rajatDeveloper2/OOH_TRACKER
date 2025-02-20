import 'package:flutter/material.dart';
import 'package:lorryzone_tracker_app/feature/auth/view/login_view.dart';
import 'package:lorryzone_tracker_app/feature/auth/view/otp_screen.dart';
import 'package:lorryzone_tracker_app/feature/auth/view/splash_view.dart';
import 'package:lorryzone_tracker_app/feature/home/view/home_view.dart';
import 'package:lorryzone_tracker_app/feature/home/view/setting_view.dart';

Map<String, Widget Function(BuildContext)> getAppRoutes() {
  Map<String, Widget Function(BuildContext)> appRoutes = {
    //  AgentListScreen.routeName: (context) {
    //       var args = ModalRoute.of(context)!.settings.arguments as Map<String, dynamic>;
    //       var listData = args['listData'] as List<AgentModel>;
    //       var caseModel = args['caseModel'] as CaseModel;

    //       return AgentListScreen(
    //         agents: listData,
    //         caseModel: caseModel,
    //       );
    //     },
    SplashView.tag: (context) => SplashView(),
    LoginView.tag: (context) => LoginView(),
    OtpScreen.tag: (context) {
      var args =
          ModalRoute.of(context)!.settings.arguments as Map<String, dynamic>;
      String mobileNumber = args['mobileNumber'];
      String realOTP = args['realOTP'];

      return OtpScreen(mobileNumber: mobileNumber, realOTP: realOTP);
    },
    HomeView.tag: (context) => HomeView(),
    SettingView.tag: (context) => SettingView(),
  };
  return appRoutes;
}
