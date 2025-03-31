package com.suayan.core.extractor;

/**
 * A bean for an ExtractRule (aka mapping).
 * 
 * @author Kyo Suayan
 *
 */
public class ExtractRule {
    
    private String sourceSubpath;
    private String sourceProperty;    
    private String targetProperty;
    private ExtractType extractType;

    public ExtractRule(String targetProperty, ExtractType eType) {
        this.extractType = eType;
        this.targetProperty = targetProperty;                        
        
        switch (extractType) {
            case TITLE:
                this.sourceProperty = "";
                this.sourceSubpath = "/jcr:content";
                break;
            case NAME:
            case PATH:
            case ALL:
            case MULTI_PROPERTY:
            case PROPERTY:
            case OBJECT_LIST:
            case OBJECT_MAP:            
                this.sourceSubpath = "";
                this.sourceProperty = "";
                break;
            default:
                break;
        }   
    }

    public ExtractRule(String targetProperty, ExtractType eType, String sourceSubpath, String sourceProperty) {
        super();
        this.targetProperty = targetProperty;        
        this.extractType = eType;
        this.sourceSubpath = sourceSubpath;
        this.sourceProperty = sourceProperty;
    }

    /**
     * @return the sourceSubpath
     */
    public String getSourceSubpath() {
        return sourceSubpath;
    }
    /**
     * @param sourceSubpath the sourceSubpath to set
     */
    public void setSourceSubpath(String sourceSubpath) {
        this.sourceSubpath = sourceSubpath;
    }
    /**
     * @return the sourceProperty
     */
    public String getSourceProperty() {
        return sourceProperty;
    }
    /**
     * @param sourceProperty the sourceProperty to set
     */
    public void setSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
    }
    /**
     * @return the targetProperty
     */
    public String getTargetProperty() {
        return targetProperty;
    }
    /**
     * @param targetProperty the targetProperty to set
     */
    public void setTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
    }
    /**
     * @return the extractType
     */
    public ExtractType getExtractType() {
        return extractType;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ExtractRule [sourceSubpath=" + sourceSubpath
                + ", sourceProperty=" + sourceProperty + ", targetProperty="
                + targetProperty + ", extractType=" + extractType + "]";
    }
}