package com.daniele.receiptocr.service;

import java.util.List;

public interface CategoryProvider {

    List<String> getAvailableCategoryCodes();

    String getCategoriesHint();
}
