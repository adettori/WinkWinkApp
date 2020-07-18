package com.application.winkwink.Utilities;

import com.google.mlkit.vision.face.Face;

public interface FaceSharer {

    public Face getFace(int id);
    public void setFace(Face face);
}
