package com.azurion.saascore.empresas.application.dto;

import org.springframework.web.multipart.MultipartFile;

public class UpdateEmpresaBrandingRequest {

    private String logoPanelUrl;
    private MultipartFile logoPanelFile;
    private Boolean clearLogoPanel;

    public String getLogoPanelUrl() {
        return logoPanelUrl;
    }

    public void setLogoPanelUrl(String logoPanelUrl) {
        this.logoPanelUrl = logoPanelUrl;
    }

    public MultipartFile getLogoPanelFile() {
        return logoPanelFile;
    }

    public void setLogoPanelFile(MultipartFile logoPanelFile) {
        this.logoPanelFile = logoPanelFile;
    }

    public Boolean getClearLogoPanel() {
        return clearLogoPanel;
    }

    public void setClearLogoPanel(Boolean clearLogoPanel) {
        this.clearLogoPanel = clearLogoPanel;
    }
}
