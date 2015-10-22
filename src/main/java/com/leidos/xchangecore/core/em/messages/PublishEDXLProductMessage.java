package com.leidos.xchangecore.core.em.messages;

import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument;

public class PublishEDXLProductMessage {

    EDXLDistributionDocument edxlProduct;
    String productType;
    String owningCore;

    public PublishEDXLProductMessage(EDXLDistributionDocument edxlProduct, String productType,
                                     String owningCore) {

        setEdxlProduct(edxlProduct);
        setProductType(productType);
        setOwningCore(owningCore);
    }

    public void setEdxlProduct(EDXLDistributionDocument edxlProduct) {

        this.edxlProduct = edxlProduct;
    }

    public void setOwningCore(String owningCore) {

        this.owningCore = owningCore;
    }

    public String getProductType() {

        return productType;
    }

    public void setProductType(String productType) {

        this.productType = productType;
    }

    public EDXLDistributionDocument getEdxlProduct() {

        return edxlProduct;
    }

    public String getOwningCore() {

        return owningCore;
    }
}
