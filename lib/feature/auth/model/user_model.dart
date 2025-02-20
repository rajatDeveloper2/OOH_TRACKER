import 'dart:convert';

// ignore_for_file: public_member_api_docs, sort_constructors_first
class UserModel {
  //   "userID": "some-user-id",
  //  "phone": "1234567890"

  String? userId;
  String? phone;
  UserModel({this.userId, this.phone});

  Map<String, dynamic> toMap() {
    return <String, dynamic>{'userId': userId, 'phone': phone};
  }

  factory UserModel.fromMap(Map<String, dynamic> map) {
    return UserModel(
      userId: map['userId'] != null ? map['userId'] as String : null,
      phone: map['phone'] != null ? map['phone'] as String : null,
    );
  }

  String toJson() => json.encode(toMap());

  factory UserModel.fromJson(String source) =>
      UserModel.fromMap(json.decode(source) as Map<String, dynamic>);
}
