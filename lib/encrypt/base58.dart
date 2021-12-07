import 'dart:convert';
import 'dart:typed_data';
import 'package:bs58/bs58.dart';

class Base58 {
  static String encode(Uint8List input) {
    return base58.encode(input);
  }

  static String encodeString(String input) {
    return base58.encode(Uint8List.fromList(utf8.encode(input)));
  }

  static String encodeUint8List(Uint8List input) {
    return base58.encode(input);
  }

  static Uint8List decodeString(String input) {
    return base58.decode(input);
  }

  static Uint8List decodeUint8List(Uint8List input) {
    return base58.decode(utf8.decode(input));
  }

  Base58._();
}
