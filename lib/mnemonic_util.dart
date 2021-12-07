class MnemonicUtil {
  static String toWordsString(List<String> words) {
    return words.join(r' ');
  }

  static List<String> toWordsList(String words) {
    return words.split(r' ');
  }
}
