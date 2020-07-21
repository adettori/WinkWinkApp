package com.application.winkwink.Utilities;

import com.google.mlkit.vision.face.Face;

/* Interface used to share data between BluetoothHostServer and LobbyFragment*/

public interface LobbySharer {

    public void setFace(Face face);

    public void setChallengerUsername(String name);
}
