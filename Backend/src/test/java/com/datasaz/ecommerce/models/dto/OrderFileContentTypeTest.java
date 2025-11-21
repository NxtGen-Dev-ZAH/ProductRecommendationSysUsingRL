package com.datasaz.ecommerce.models.dto;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderFileContentTypeTest {

//    @Test
//    void testMediaTypeAssociations() {
//        assertEquals(MediaType.APPLICATION_PDF, OrderFileContentType.PDF.getMediaType());
//        assertEquals(MediaType.TEXT_HTML, OrderFileContentType.HTML.getMediaType());
//        assertEquals(MediaType.APPLICATION_JSON, OrderFileContentType.JSON.getMediaType());
//        assertEquals(MediaType.APPLICATION_XML, OrderFileContentType.XML.getMediaType());
//        assertEquals(MediaType.TEXT_PLAIN, OrderFileContentType.TXT.getMediaType());
//    }

    @ParameterizedTest
    @EnumSource(OrderFileContentType.class)
    void testGetFileNameWithNormalPrefix(OrderFileContentType contentType) {
        String prefix = "test";
        String expected = prefix + "." + contentType.name().toLowerCase();
        assertEquals(expected, contentType.getFileName(prefix));
    }

    @ParameterizedTest
    @EnumSource(OrderFileContentType.class)
    void testGetFileNameWithEmptyPrefix(OrderFileContentType contentType) {
        assertEquals("." + contentType.name().toLowerCase(), contentType.getFileName(""));
    }

    @ParameterizedTest
    @EnumSource(OrderFileContentType.class)
    void testGetFileNameWithNullPrefix(OrderFileContentType contentType) {
        assertEquals("null." + contentType.name().toLowerCase(),
                contentType.getFileName(null));
    }

//    @Test
//    void testEnumValues() {
//        OrderFileContentType[] values = OrderFileContentType.values();
//        assertEquals(15, values.length);
//        assertArrayEquals(
//                new OrderFileContentType[]{
//                        OrderFileContentType.PDF,
//                        OrderFileContentType.HTML,
//                        OrderFileContentType.JSON,
//                        OrderFileContentType.XML,
//                        OrderFileContentType.TXT_XML,
//                        OrderFileContentType.GIF,
//                        OrderFileContentType.JPEG,
//                        OrderFileContentType.PNG,
//                        OrderFileContentType.PPT,
//                        OrderFileContentType.OFFICE,
//                        OrderFileContentType.TXT,
//                        OrderFileContentType.XLS,
//                        OrderFileContentType.MSG,
//                        OrderFileContentType.ZIP7,
//                        OrderFileContentType.ZIP
//                },
//                values
//        );
//    }
//
//    @Test
//    void testValueOf() {
//        assertEquals(OrderFileContentType.PDF, OrderFileContentType.valueOf("PDF"));
//        assertEquals(OrderFileContentType.HTML, OrderFileContentType.valueOf("HTML"));
//        assertEquals(OrderFileContentType.JSON, OrderFileContentType.valueOf("JSON"));
//        assertEquals(OrderFileContentType.XML, OrderFileContentType.valueOf("XML"));
//        assertEquals(OrderFileContentType.TXT, OrderFileContentType.valueOf("TXT"));
//        assertThrows(IllegalArgumentException.class, () -> OrderFileContentType.valueOf("INVALID"));
//    }
}