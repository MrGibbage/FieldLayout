package fieldlayout.skipmorrow.com.fieldlayout;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by skip on 4/11/2015.
 *
 * All dimensions internally are in meters. Dimensions can be converted to other units for display
 * purposes.
 *
 * Field corners are laid out like this
 *
 *   <   Width     >
 *   1             2
 *                  End Zone
 *   3             4---
 *                   ^
 *
 *                   Length (does not include endzones
 *
 *
 *                   V
 *   5             6---
 *
 *   7             8
 *
 *  Start Corner        End Corner Options
 *  1                   7, 8
 *  2                   7, 8
 *  3                   5, 6
 *  4                   5, 6
 */
enum Unit {METERS, YARDS}

public class FieldClass implements Comparable<FieldClass> {
    // field length without the endzones
    private Float _fFieldLength;

    // field width
    private Float _fFieldWidth;

    // endzone length
    private Float _fEndZoneLength;

    // true if the field has endzones
    private Boolean _bHasEndZone;

    private Unit _unit;

    private String _strFieldType;

    private int _index;

    private FieldClass(String strFieldType,
                       Float fFieldLength,
                       Float fFieldWidth,
                       Float fEndZoneLength,
                       Unit unit,
                       Boolean bHasEndZone,
                       int index) {
        _fFieldLength = fFieldLength;
        _fFieldWidth = fFieldWidth;
        _fEndZoneLength = fEndZoneLength;
        _bHasEndZone = bHasEndZone;
        _strFieldType = strFieldType;
        _unit = unit;
        _index = index;
    }

    @Override
    public int compareTo(@NonNull FieldClass fc) {
        return this._strFieldType.compareTo(fc._strFieldType);
    }

    public static ArrayList<FieldClass> BuildFieldList() {
        ArrayList<FieldClass> fieldList = new ArrayList<>();
        fieldList.add(new FieldClass("Football (Canadian)",     110f,   65f,   10f,   Unit.YARDS,   true,   0));
        fieldList.add(new FieldClass("Football (US)",           100f,   53.3f, 10f,   Unit.YARDS,   true,   1));
        fieldList.add(new FieldClass("Soccer (U10)",            70f,    40f,   0f,    Unit.YARDS,   false,  2));
        fieldList.add(new FieldClass("Soccer (U12)",            80f,    50f,   0f,    Unit.YARDS,   false,  3));
        fieldList.add(new FieldClass("Soccer (U14)",            100f,   60f,   0f,    Unit.YARDS,   false,  4));
        fieldList.add(new FieldClass("Ultimate Frisbee (AUDL)", 80f,    53.3f, 20f,   Unit.YARDS,   true,   5));
        fieldList.add(new FieldClass("Ultimate Frisbee (MLU)",  80f,    53.3f, 20f,   Unit.YARDS,   true,   6));
        fieldList.add(new FieldClass("Ultimate Frisbee (USAU)", 70f,    40f,   25f,   Unit.YARDS,   true,   7));

        Collections.sort(fieldList);
        return fieldList;
    }

    // Returns a FieldClass object with the index number of the provided parameter
    // if the parameter is not found, returns null
    public static FieldClass GetFieldWithIndex(int idx) {
        ArrayList<FieldClass> fcl = BuildFieldList();
        for (int i = 0; i < fcl.size(); i++) {
            if (fcl.get(i)._index == idx) return fcl.get(i);
        }
        return null;
    }

    public static ArrayList<String> GetFieldtypeList() {
        ArrayList<String> typeList = new ArrayList<>();
        ArrayList<FieldClass> fcl = BuildFieldList();
        for (int i = 0; i < fcl.size(); i++) {
            typeList.add(fcl.get(i)._strFieldType);
        }
        return typeList;
    }

    public Float get_fFieldLength() {
        return _fFieldLength;
    }

    public Float get_fFieldWidth() {
        return _fFieldWidth;
    }

    public Float get_fFieldLengthInMeters() {
        return this._unit == Unit.METERS ? _fFieldLength : _fFieldLength * 0.9144f;
    }

    public Float get_fFieldWidthInMeters() {
        return this._unit == Unit.METERS ? _fFieldWidth : _fFieldWidth * 0.9144f;
    }

    public Float get_fFieldLengthInYards() {
        return this._unit == Unit.YARDS ? _fFieldLength : _fFieldLength * 1.09361f;
    }

    public Float get_fFieldWidthInYards() {
        return this._unit == Unit.YARDS ? _fFieldWidth : _fFieldWidth * 1.09361f;
    }

    public int get_index() {
        return _index;
    }

    public Float get_fEndZoneLength() {
        return _fEndZoneLength;
    }

    public Float get_fEndZoneLengthInMeters() {
        return this._unit == Unit.METERS ? _fEndZoneLength : _fEndZoneLength * 0.9144f;
    }

    public Unit get_unit() {
        return _unit;
    }

    public String get_strFieldType() {
        return _strFieldType;
    }

    public Boolean get_bHasEndZone() {
        return _bHasEndZone;
    }

}
