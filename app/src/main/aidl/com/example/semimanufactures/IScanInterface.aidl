package com.example.semimanufactures;

interface IScanInterface {
    void sendKeyEvent(in KeyEvent key);
    void scan();
    void stop();
    int getScannerModel();
}
