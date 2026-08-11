

// -----( IS Java Code Template v1.2

import com.wm.data.*;
import com.wm.util.Values;
import com.wm.app.b2b.server.Service;
import com.wm.app.b2b.server.ServiceException;
// --- <<IS-START-IMPORTS>> ---
// --- <<IS-END-IMPORTS>> ---

public final class ediDemo

{
	// ---( internal utility methods )---

	final static ediDemo _instance = new ediDemo();

	static ediDemo _newInstance() { return new ediDemo(); }

	static ediDemo _cast(Object o) { return (ediDemo)o; }

	// ---( server methods )---




	public static final void mapEDI850ToJSONJava (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(mapEDI850ToJSONJava)>> ---
		// @sigtype java 3.5
		// [i] field:0:required ediDocument
		// [o] field:0:required jsonOutput
		// [o] field:0:required isValid
		IDataCursor pc = pipeline.getCursor();
		String ediDocument = IDataUtil.getString(pc, "ediDocument");
		pc.destroy();
		 
		String jsonOutput = "";
		String isValidFlag = "false";
		 
		try {
		    if (ediDocument == null || ediDocument.trim().length() == 0) {
		        throw new Exception("EMPTY_DOCUMENT|ediDocument is null or empty");
		    }
		 
		    // ---- Split into segments, strip newlines, drop empties -------------
		    String flat = ediDocument.replace("\r", "").replace("\n", "");
		    java.util.List rawSegs = new java.util.ArrayList();
		    int start = 0;
		    for (int i = 0; i < flat.length(); i++) {
		        if (flat.charAt(i) == '~') {
		            String s = flat.substring(start, i).trim();
		            if (s.length() > 0) rawSegs.add(s);
		            start = i + 1;
		        }
		    }
		    String tail = flat.substring(start).trim();
		    if (tail.length() > 0) rawSegs.add(tail);
		 
		    int segCount = rawSegs.size();
		    if (segCount == 0) {
		        throw new Exception("NO_SEGMENTS|No segments found in document");
		    }
		 
		    // ---- Pre-split every segment, padded to 20 elements ----------------
		    // Padding means we can index any element without bounds checks.
		    String[][] seg = new String[segCount][];
		    for (int i = 0; i < segCount; i++) {
		        String line = (String) rawSegs.get(i);
		        java.util.List parts = new java.util.ArrayList();
		        int p = 0;
		        for (int j = 0; j < line.length(); j++) {
		            if (line.charAt(j) == '*') {
		                parts.add(line.substring(p, j));
		                p = j + 1;
		            }
		        }
		        parts.add(line.substring(p));
		        String[] arr = new String[20];
		        for (int k = 0; k < 20; k++) {
		            arr[k] = (k < parts.size()) ? (String) parts.get(k) : "";
		        }
		        seg[i] = arr;
		    }
		 
		    // ---- Locate key segments ------------------------------------------
		    int isaIdx = -1, gsIdx = -1, stIdx = -1, seIdx = -1;
		    int geIdx = -1, ieaIdx = -1, begIdx = -1, cttIdx = -1;
		    int po1Count = 0;
		    long po1QtySum = 0L;
		 
		    for (int i = 0; i < segCount; i++) {
		        String tag = seg[i][0];
		        if (tag.equals("ISA") && isaIdx < 0) isaIdx = i;
		        else if (tag.equals("GS") && gsIdx < 0) gsIdx = i;
		        else if (tag.equals("ST") && stIdx < 0) stIdx = i;
		        else if (tag.equals("SE") && seIdx < 0) seIdx = i;
		        else if (tag.equals("GE") && geIdx < 0) geIdx = i;
		        else if (tag.equals("IEA") && ieaIdx < 0) ieaIdx = i;
		        else if (tag.equals("BEG") && begIdx < 0) begIdx = i;
		        else if (tag.equals("CTT") && cttIdx < 0) cttIdx = i;
		        else if (tag.equals("PO1")) {
		            po1Count++;
		            try {
		                po1QtySum += (long) Double.parseDouble(seg[i][2]);
		            } catch (Exception nfe) {
		                throw new Exception("BAD_QUANTITY|PO102 is not numeric on line " + seg[i][1]);
		            }
		        }
		    }
		 
		    if (isaIdx < 0) throw new Exception("MISSING_ISA|No ISA segment found");
		    if (stIdx < 0 || seIdx < 0) throw new Exception("MISSING_ST_SE|No ST/SE envelope found");
		    if (begIdx < 0) throw new Exception("MISSING_BEG|No BEG segment found");
		 
		    // ---- Validation pass ----------------------------------------------
		    // 1. SE01 segment count (ST through SE inclusive)
		    int actualSpan = seIdx - stIdx + 1;
		    int declaredSpan;
		    try {
		        declaredSpan = Integer.parseInt(seg[seIdx][1].trim());
		    } catch (Exception nfe) {
		        throw new Exception("SE01_NOT_NUMERIC|SE01 is not a number: " + seg[seIdx][1]);
		    }
		    if (declaredSpan != actualSpan) {
		        throw new Exception("SE01_MISMATCH|SE01 segment count " + declaredSpan
		                + " does not match actual ST..SE span of " + actualSpan);
		    }
		 
		    // 2. Control number matching
		    if (!seg[stIdx][2].equals(seg[seIdx][2])) {
		        throw new Exception("ST_SE_CONTROL_MISMATCH|ST02 " + seg[stIdx][2]
		                + " does not match SE02 " + seg[seIdx][2]);
		    }
		    if (ieaIdx >= 0 && !seg[isaIdx][13].equals(seg[ieaIdx][2])) {
		        throw new Exception("ISA_IEA_CONTROL_MISMATCH|ISA13 " + seg[isaIdx][13]
		                + " does not match IEA02 " + seg[ieaIdx][2]);
		    }
		    if (gsIdx >= 0 && geIdx >= 0 && !seg[gsIdx][6].equals(seg[geIdx][2])) {
		        throw new Exception("GS_GE_CONTROL_MISMATCH|GS06 " + seg[gsIdx][6]
		                + " does not match GE02 " + seg[geIdx][2]);
		    }
		 
		    // 3 & 4. CTT line count and quantity hash
		    if (cttIdx >= 0) {
		        int cttLines;
		        try {
		            cttLines = Integer.parseInt(seg[cttIdx][1].trim());
		        } catch (Exception nfe) {
		            throw new Exception("CTT01_NOT_NUMERIC|CTT01 is not a number: " + seg[cttIdx][1]);
		        }
		        if (cttLines != po1Count) {
		            throw new Exception("CTT_LINE_COUNT_MISMATCH|CTT01 line count " + cttLines
		                    + " does not match " + po1Count + " PO1 loops");
		        }
		        if (seg[cttIdx][2].length() > 0) {
		            long cttQty;
		            try {
		                cttQty = (long) Double.parseDouble(seg[cttIdx][2].trim());
		            } catch (Exception nfe) {
		                throw new Exception("CTT02_NOT_NUMERIC|CTT02 is not a number: " + seg[cttIdx][2]);
		            }
		            if (cttQty != po1QtySum) {
		                throw new Exception("CTT_QUANTITY_MISMATCH|CTT02 quantity hash " + cttQty
		                        + " does not match PO102 sum " + po1QtySum);
		            }
		        }
		    }
		 
		    // ---- Mapping pass -------------------------------------------------
		    // Envelope. ISA06/ISA08 are space padded to 15 chars - trim trailing.
		    String senderId = seg[isaIdx][6].trim();
		    String receiverId = seg[isaIdx][8].trim();
		    String ctrlNumber = seg[isaIdx][13];
		    String isaDate = seg[isaIdx][9];   // YYMMDD
		    String isaTime = seg[isaIdx][10];  // HHMM
		    String usageInd = seg[isaIdx][15];
		 
		    String interchangeDate = (isaDate.length() == 6)
		            ? "20" + isaDate.substring(0, 2) + "-" + isaDate.substring(2, 4) + "-" + isaDate.substring(4, 6)
		            : isaDate;
		    String interchangeTime = (isaTime.length() == 4)
		            ? isaTime.substring(0, 2) + ":" + isaTime.substring(2, 4)
		            : isaTime;
		 
		    // Header. BEG05 is CCYYMMDD (note: different format from ISA09).
		    String poNumber = seg[begIdx][3];
		    String begDate = seg[begIdx][5];
		    String poDate = (begDate.length() == 8)
		            ? begDate.substring(0, 4) + "-" + begDate.substring(4, 6) + "-" + begDate.substring(6, 8)
		            : begDate;
		 
		    String purposeCode = seg[begIdx][1];
		    String purpose = purposeCode.equals("00") ? "Original"
		            : purposeCode.equals("01") ? "Cancellation"
		            : purposeCode.equals("04") ? "Change"
		            : purposeCode.equals("05") ? "Replace"
		            : purposeCode;
		 
		    String typeCode = seg[begIdx][2];
		    String poType = typeCode.equals("NE") ? "New Order"
		            : typeCode.equals("RL") ? "Release or Delivery Order"
		            : typeCode.equals("SA") ? "Stand-alone Order"
		            : typeCode;
		 
		    // REF segments, keyed by REF01 qualifier.
		    String deptNumber = "";
		    String vendorNumber = "";
		    String requestedDelivery = "";
		    for (int i = 0; i < segCount; i++) {
		        if (seg[i][0].equals("REF")) {
		            if (seg[i][1].equals("DP")) deptNumber = seg[i][2];
		            else if (seg[i][1].equals("IA")) vendorNumber = seg[i][2];
		        } else if (seg[i][0].equals("DTM") && seg[i][1].equals("002")) {
		            String d = seg[i][2];
		            requestedDelivery = (d.length() == 8)
		                    ? d.substring(0, 4) + "-" + d.substring(4, 6) + "-" + d.substring(6, 8)
		                    : d;
		        }
		    }
		 
		    // N1 loops. N3/N4 belong to the most recent N1.
		    String[] btParty = new String[]{"", "", "", "", "", "", ""};
		    String[] stParty = new String[]{"", "", "", "", "", "", ""};
		    // slots: 0 name, 1 idCode, 2 street, 3 city, 4 state, 5 postalCode, 6 country
		    String[] current = null;
		    for (int i = 0; i < segCount; i++) {
		        String tag = seg[i][0];
		        if (tag.equals("N1")) {
		            if (seg[i][1].equals("BT")) current = btParty;
		            else if (seg[i][1].equals("ST")) current = stParty;
		            else current = null;
		            if (current != null) {
		                current[0] = seg[i][2];
		                current[1] = seg[i][4];
		            }
		        } else if (tag.equals("N3") && current != null) {
		            current[2] = seg[i][1];
		        } else if (tag.equals("N4") && current != null) {
		            current[3] = seg[i][1];
		            current[4] = seg[i][2];
		            current[5] = seg[i][3];
		            current[6] = seg[i][4];
		        }
		    }
		 
		    // PO1 loops. The PID immediately following a PO1 supplies its description.
		    StringBuffer items = new StringBuffer();
		    double orderTotal = 0.0d;
		    boolean firstItem = true;
		    for (int i = 0; i < segCount; i++) {
		        if (!seg[i][0].equals("PO1")) continue;
		 
		        int lineNumber = 0;
		        try { lineNumber = Integer.parseInt(seg[i][1].trim()); } catch (Exception ignore) { }
		        int quantity = 0;
		        try { quantity = (int) Double.parseDouble(seg[i][2].trim()); } catch (Exception ignore) { }
		        double unitPrice = 0.0d;
		        try { unitPrice = Double.parseDouble(seg[i][4].trim()); } catch (Exception ignore) { }
		 
		        String uom = seg[i][3];
		        // Qualifier gates the value: read PO107 only when PO106 is UP.
		        String upc = seg[i][6].equals("UP") ? seg[i][7] : "";
		        String vendorPart = seg[i][8].equals("VN") ? seg[i][9] : "";
		 
		        String description = "";
		        if (i + 1 < segCount && seg[i + 1][0].equals("PID")) {
		            description = seg[i + 1][5];
		        }
		 
		        double lineTotal = Math.round(quantity * unitPrice * 100.0d) / 100.0d;
		        orderTotal += lineTotal;
		 
		        if (!firstItem) items.append(",");
		        firstItem = false;
		        items.append("{");
		        items.append("\"lineNumber\":").append(lineNumber).append(",");
		        items.append("\"quantity\":").append(quantity).append(",");
		        items.append("\"unitOfMeasure\":\"").append(uom.replace("\\", "\\\\").replace("\"", "\\\"")).append("\",");
		        items.append("\"unitPrice\":").append(String.format("%.2f", Double.valueOf(unitPrice))).append(",");
		        items.append("\"upc\":\"").append(upc).append("\",");
		        items.append("\"vendorPartNumber\":\"").append(vendorPart.replace("\\", "\\\\").replace("\"", "\\\"")).append("\",");
		        items.append("\"description\":\"").append(description.replace("\\", "\\\\").replace("\"", "\\\"")).append("\",");
		        items.append("\"lineTotal\":").append(String.format("%.2f", Double.valueOf(lineTotal)));
		        items.append("}");
		    }
		    orderTotal = Math.round(orderTotal * 100.0d) / 100.0d;
		 
		    int totalLineItems = po1Count;
		    long totalQuantity = po1QtySum;
		    if (cttIdx >= 0) {
		        try { totalLineItems = Integer.parseInt(seg[cttIdx][1].trim()); } catch (Exception ignore) { }
		        try { totalQuantity = (long) Double.parseDouble(seg[cttIdx][2].trim()); } catch (Exception ignore) { }
		    }
		 
		    // ---- Serialise. Key order matches samples/expected-output.json ----
		    StringBuffer j = new StringBuffer();
		    j.append("{\"purchaseOrder\":{");
		 
		    j.append("\"interchange\":{");
		    j.append("\"senderId\":\"").append(senderId).append("\",");
		    j.append("\"receiverId\":\"").append(receiverId).append("\",");
		    j.append("\"controlNumber\":\"").append(ctrlNumber).append("\",");
		    j.append("\"date\":\"").append(interchangeDate).append("\",");
		    j.append("\"time\":\"").append(interchangeTime).append("\",");
		    j.append("\"usageIndicator\":\"").append(usageInd).append("\"");
		    j.append("},");
		 
		    j.append("\"poNumber\":\"").append(poNumber).append("\",");
		    j.append("\"poDate\":\"").append(poDate).append("\",");
		    j.append("\"purpose\":\"").append(purpose).append("\",");
		    j.append("\"poType\":\"").append(poType).append("\",");
		 
		    j.append("\"references\":{");
		    j.append("\"departmentNumber\":\"").append(deptNumber).append("\",");
		    j.append("\"internalVendorNumber\":\"").append(vendorNumber).append("\"");
		    j.append("},");
		 
		    j.append("\"requestedDeliveryDate\":\"").append(requestedDelivery).append("\",");
		 
		    j.append("\"billTo\":{");
		    j.append("\"name\":\"").append(btParty[0].replace("\\", "\\\\").replace("\"", "\\\"")).append("\",");
		    j.append("\"idCode\":\"").append(btParty[1]).append("\",");
		    j.append("\"address\":{");
		    j.append("\"street\":\"").append(btParty[2].replace("\\", "\\\\").replace("\"", "\\\"")).append("\",");
		    j.append("\"city\":\"").append(btParty[3]).append("\",");
		    j.append("\"state\":\"").append(btParty[4]).append("\",");
		    j.append("\"postalCode\":\"").append(btParty[5]).append("\",");
		    j.append("\"country\":\"").append(btParty[6]).append("\"");
		    j.append("}},");
		 
		    j.append("\"shipTo\":{");
		    j.append("\"name\":\"").append(stParty[0].replace("\\", "\\\\").replace("\"", "\\\"")).append("\",");
		    j.append("\"idCode\":\"").append(stParty[1]).append("\",");
		    j.append("\"address\":{");
		    j.append("\"street\":\"").append(stParty[2].replace("\\", "\\\\").replace("\"", "\\\"")).append("\",");
		    j.append("\"city\":\"").append(stParty[3]).append("\",");
		    j.append("\"state\":\"").append(stParty[4]).append("\",");
		    j.append("\"postalCode\":\"").append(stParty[5]).append("\",");
		    j.append("\"country\":\"").append(stParty[6]).append("\"");
		    j.append("}},");
		 
		    j.append("\"lineItems\":[").append(items.toString()).append("],");
		 
		    j.append("\"summary\":{");
		    j.append("\"totalLineItems\":").append(totalLineItems).append(",");
		    j.append("\"totalQuantity\":").append(totalQuantity).append(",");
		    j.append("\"orderTotal\":").append(String.format("%.2f", Double.valueOf(orderTotal)));
		    j.append("}");
		 
		    j.append("}}");
		 
		    jsonOutput = j.toString();
		    isValidFlag = "true";
		 
		} catch (Exception e) {
		    // Validation failures and parse errors both land here. Messages are
		    // encoded as "CODE|message" so the error object stays structured.
		    String raw = (e.getMessage() == null) ? "UNKNOWN|Unhandled error" : e.getMessage();
		    String code = "MAPPING_ERROR";
		    String message = raw;
		    int bar = raw.indexOf('|');
		    if (bar > 0) {
		        code = raw.substring(0, bar);
		        message = raw.substring(bar + 1);
		    }
		    message = message.replace("\\", "\\\\").replace("\"", "\\\"");
		    jsonOutput = "{\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}";
		    isValidFlag = "false";
		}
		 
		// ---- Write outputs back to the pipeline --------------------------------
		IDataCursor out = pipeline.getCursor();
		IDataUtil.put(out, "jsonOutput", jsonOutput);
		IDataUtil.put(out, "isValid", isValidFlag);
		out.destroy();
		// --- <<IS-END>> ---

                
	}
}

