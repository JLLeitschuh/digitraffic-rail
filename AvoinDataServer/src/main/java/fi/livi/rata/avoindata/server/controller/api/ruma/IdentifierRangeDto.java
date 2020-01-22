package fi.livi.rata.avoindata.server.controller.api.ruma;

import io.swagger.annotations.ApiModelProperty;

import java.util.Set;

public class IdentifierRangeDto {

    @ApiModelProperty("Identifier of element")
    public final String elementId;

    @ApiModelProperty("Identifier of element 1 in element pair")
    public final String elementPairId1;

    @ApiModelProperty("Identifier of element 2 in element pair")
    public final String elementPairId2;

    @ApiModelProperty("Element ranges")
    public final Set<ElementRangeDto> elementRanges;

    public IdentifierRangeDto(
            final String elementId,
            final String elementPairId1,
            final String elementPairId2,
            final Set<ElementRangeDto> elementRanges)
    {
        this.elementId = elementId;
        this.elementPairId1 = elementPairId1;
        this.elementPairId2 = elementPairId2;
        this.elementRanges = elementRanges;
    }
}