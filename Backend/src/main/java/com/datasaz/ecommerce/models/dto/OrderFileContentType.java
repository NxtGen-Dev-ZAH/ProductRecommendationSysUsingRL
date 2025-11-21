package com.datasaz.ecommerce.models.dto;

import lombok.Getter;
import org.springframework.http.MediaType;

@Getter
public enum OrderFileContentType {
    //PDF(MediaType.APPLICATION_PDF),
    //HTML(MediaType.TEXT_HTML),
    //JSON(MediaType.APPLICATION_JSON),
    //XML(MediaType.APPLICATION_XML),
    //TXT_XML(MediaType.valueOf("text/xml")),
    GIF(MediaType.IMAGE_GIF),
    JPEG(MediaType.IMAGE_JPEG),
    PNG(MediaType.IMAGE_PNG);
    //PPT(MediaType.valueOf("application/vnd.ms-powerpoint")),
    //OFFICE(MediaType.valueOf("application/x-tika-ooxml")),
    //TXT(MediaType.TEXT_PLAIN),
    //XLS(MediaType.valueOf("application/x-tika-msoffice")),
    //MSG(MediaType.valueOf("application/vnd.ms-outlook")),
    //ZIP7(MediaType.valueOf("application/x-7z-compressed")),
    //ZIP(MediaType.valueOf("application/zip"));

    private final MediaType mediaType;

    OrderFileContentType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getFileName(String prefix) {
        return String.format("%s.%s", prefix, this.name().toLowerCase());
    }

    public String getValue() {
        return this.name();
    }

}