package com.dankposter.dto.d3vd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class D3vdResponse {
    private int count;
    private List<D3vdPost> memes;
}
