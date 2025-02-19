

import 'package:flutter/material.dart';
import 'package:lorryzone_tracker_app/feature/auth/view/splash_view.dart';

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
    SplashView.tag : (context)=> SplashView()
   

  };
  return appRoutes;
}
