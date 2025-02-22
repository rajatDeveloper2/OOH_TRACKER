import 'package:flutter/material.dart';
import 'package:lorryzone_tracker_app/res/colors.dart';

bool isLoadingShown = false;

void showLoading(BuildContext context) {
  isLoadingShown = true;
  showDialog(
    context: context,
    barrierDismissible: false,
    builder: (BuildContext context) {
      return Dialog(
        backgroundColor: Colors.transparent,
        insetPadding: const EdgeInsets.all(0),
        // ignore: deprecated_member_use
        child: WillPopScope(
          child: const Center(
            child: CircularProgressIndicator(color: AppColors.whiteColor),
          ),
          onWillPop: () async {
            return false;
          },
        ),
      );
    },
  );
}

void hideLoading(BuildContext context) {
  if (isLoadingShown) {
    Navigator.of(context).pop();
    isLoadingShown = false;
  }
}
