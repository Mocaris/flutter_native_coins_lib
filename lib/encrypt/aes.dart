import 'dart:convert';
import 'dart:typed_data';

import 'package:encrypt/encrypt.dart' as ept;

import 'base58.dart';

extension EncryptedExt on ept.Encrypted {
  String get base58 => Base58.encodeUint8List(bytes);
}

class AESEncryption {
//AES/ECB/PKCS5Padding
  final ept.Encrypter _aesEncrypt;

  AESEncryption(String passWord) : _aesEncrypt = ept.Encrypter(ept.AES(ept.Key.fromUtf8(passWord), mode: ept.AESMode.ecb));

  final _iv = ept.IV.fromLength(16);

  String encryptString(String str) {
    return String.fromCharCodes(_aesEncrypt.encrypt(str, iv: _iv).bytes);
  }

  String decryptString(String encryptStr) {
    var bytes = _aesEncrypt.decryptBytes(ept.Encrypted(Uint8List.fromList(utf8.encode(encryptStr))), iv: _iv);
    return String.fromCharCodes(bytes);
  }

  String encrypt58(String str) {
    return _aesEncrypt.encrypt(str, iv: _iv).base58;
  }

  String decrypt58(String encryptStr) {
    var bytes = _aesEncrypt.decryptBytes(ept.Encrypted(Base58.decodeString(encryptStr)), iv: _iv);
    return String.fromCharCodes(bytes);
  }
}
