import 'dart:convert';
import 'dart:typed_data';
import 'package:bs58/bs58.dart';

final _base58 = base58;

String encode(Uint8List input) {
  return _base58.encode(input);
}

String encodeString(String input) {
  return _base58.encode(Uint8List.fromList(utf8.encode(input)));
}

String encodeUint8List(Uint8List input) {
  return _base58.encode(input);
}

Uint8List decodeString(String input) {
  return _base58.decode(input);
}

Uint8List decodeUint8List(Uint8List input) {
  return _base58.decode(utf8.decode(input));
}
