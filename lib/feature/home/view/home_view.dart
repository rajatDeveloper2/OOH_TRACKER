import 'package:flutter/material.dart';
import 'package:lorryzone_tracker_app/res/colors.dart';

class HomeView extends StatefulWidget {
  static const String tag = "home-view";
  const HomeView({super.key});

  @override
  State<HomeView> createState() => _HomeViewState();
}

class _HomeViewState extends State<HomeView> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(backgroundColor: AppColors.darkGreyColor);
  }
}
