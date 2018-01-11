package com.starmicronics.starprntsdk.functions;

import com.starmicronics.starioextension.IScaleCommandBuilder;
import com.starmicronics.starioextension.StarIoExt;
import com.starmicronics.starioextension.StarIoExt.ScaleModel;

public class ScaleFunctions {
    public static byte[] createZeroClear() {
        IScaleCommandBuilder builder = StarIoExt.createScaleCommandBuilder(ScaleModel.APS10);
//      IScaleCommandBuilder builder = StarIoExt.createScaleCommandBuilder(ScaleModel.APS12);
//      IScaleCommandBuilder builder = StarIoExt.createScaleCommandBuilder(ScaleModel.APS20);

        builder.appendZeroClear();

        return builder.getPassThroughCommands();
    }

    public static byte[] createUnitChange() {
        IScaleCommandBuilder builder = StarIoExt.createScaleCommandBuilder(ScaleModel.APS10);
//      IScaleCommandBuilder builder = StarIoExt.createScaleCommandBuilder(ScaleModel.APS12);
//      IScaleCommandBuilder builder = StarIoExt.createScaleCommandBuilder(ScaleModel.APS20);

        builder.appendUnitChange();

        return builder.getPassThroughCommands();
    }
}
