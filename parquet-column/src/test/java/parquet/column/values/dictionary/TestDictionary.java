package parquet.column.values.dictionary;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import parquet.bytes.BytesInput;
import parquet.column.Dictionary;
import parquet.column.Encoding;
import parquet.column.page.DictionaryPage;
import parquet.column.values.ValuesReader;
import parquet.column.values.plain.BinaryPlainValuesReader;
import parquet.column.values.plain.PlainValuesReader;
import parquet.io.api.Binary;

public class TestDictionary {

  @Test
  public void testDict() throws IOException {
    int COUNT = 100;
    DictionaryValuesWriter cw = new DictionaryValuesWriter(10000, 10000);
    for (int i = 0; i < COUNT; i++) {
      cw.writeBytes(Binary.fromString("a" + i % 10));
    }
    final BytesInput bytes1 = BytesInput.copy(cw.getBytes());
    cw.reset();
    for (int i = 0; i < COUNT; i++) {
      cw.writeBytes(Binary.fromString("b" + i % 10));
    }
    final BytesInput bytes2 = BytesInput.copy(cw.getBytes());
    cw.reset();

    final DictionaryPage dictionaryPage = cw.createDictionaryPage().copy();
    final DictionaryValuesReader cr = new DictionaryValuesReader();
    final Dictionary dictionary = Encoding.PLAIN_DICTIONARY.initDictionary(dictionaryPage);
//    System.out.println(dictionary);
    cr.setDictionary(dictionary);

    cr.initFromPage(COUNT, bytes1.toByteArray(), 0);
    for (int i = 0; i < COUNT; i++) {
      final String str = cr.readBytes().toStringUsingUTF8();
      Assert.assertEquals("a" + i % 10, str);
    }

    cr.initFromPage(COUNT, bytes2.toByteArray(), 0);
    for (int i = 0; i < COUNT; i++) {
      final String str = cr.readBytes().toStringUsingUTF8();
      Assert.assertEquals("b" + i % 10, str);
    }

  }

  @Test
  public void testDictInefficiency() throws IOException {
    int COUNT = 40000;
    DictionaryValuesWriter cw = new DictionaryValuesWriter(200000000, 1100000);
    for (int i = 0; i < COUNT; i++) {
      cw.writeBytes(Binary.fromString("a" + i ));
    }
    final BytesInput bytes1 = BytesInput.copy(cw.getBytes());
    final Encoding encoding1 = cw.getEncoding();
    System.out.println(encoding1 + "  " + bytes1.size());
    cw.reset();
    for (int i = 0; i < COUNT; i++) {
      cw.writeBytes(Binary.fromString("b" + i ));
    }
    final BytesInput bytes2 = BytesInput.copy(cw.getBytes());
    final Encoding encoding2 = cw.getEncoding();
    System.out.println(encoding2 + "  " + bytes2.size());
    cw.reset();

    final DictionaryPage dictionaryPage = cw.createDictionaryPage();
    Dictionary dictionary = null;
    ValuesReader cr;
    if (dictionaryPage != null) {
      System.out.println("dict byte size: " + dictionaryPage.getBytes().size());
      dictionary = Encoding.PLAIN_DICTIONARY.initDictionary(dictionaryPage);

      cr = new DictionaryValuesReader();
      cr.setDictionary(dictionary);
    } else {
      cr = new BinaryPlainValuesReader();
    }

    if (dictionary != null && encoding1 == Encoding.PLAIN_DICTIONARY) {
      cr = new DictionaryValuesReader();
      cr.setDictionary(dictionary);
    } else {
      cr = new BinaryPlainValuesReader();
    }

    cr.initFromPage(COUNT, bytes1.toByteArray(), 0);
    for (int i = 0; i < COUNT; i++) {
      final String str = cr.readBytes().toStringUsingUTF8();
      Assert.assertEquals("a" + i, str);
    }

    if (dictionary != null && encoding2 == Encoding.PLAIN_DICTIONARY) {
      cr = new DictionaryValuesReader();
      cr.setDictionary(dictionary);
    } else {
      cr = new BinaryPlainValuesReader();
    }
    cr.initFromPage(COUNT, bytes2.toByteArray(), 0);
    for (int i = 0; i < COUNT; i++) {
      final String str = cr.readBytes().toStringUsingUTF8();
      Assert.assertEquals("b" + i, str);
    }

  }
}
