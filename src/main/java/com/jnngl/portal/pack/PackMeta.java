package com.jnngl.portal.pack;

import com.google.gson.annotations.SerializedName;

public record PackMeta(@SerializedName("pack_format") int format, String description) {
}
