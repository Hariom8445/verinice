package sernet.verinice.web;

import sernet.gs.web.Util;

import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

/**
 * ManagedBean to show error and info messages.
 * 
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
@ManagedBean(name = "message")
@SessionScoped
public class MessageBean {

    private String info;
    
    private String error;
    
    private Locale locale = null;

    @PostConstruct
    public void init(){
        FacesContext context = FacesContext.getCurrentInstance();
        if (locale == null) {
            locale = context.getViewRoot().getLocale();
        } else {
            context.getViewRoot().setLocale(locale);
        }
    }

    public void showInfo() {
        Util.addInfo("massagePanel", getInfo()); //$NON-NLS-1$
        setInfo(null);
    }
    
    /**
     * Returns an checked icon if the language match the active setting.
     */
    public String getIcon(String language){
        if(locale.getLanguage().equalsIgnoreCase(language)){
            return "fa fa-check-circle-o";
        } else {
            return "fa fa-circle-o";
        }
    }

    public void showError() {
        Util.addError("massagePanel", getError()); //$NON-NLS-1$
        setError(null);
    }
    
    public String getInfo() {
        return info;
    }

    public void setInfo(String message) {
        this.info = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
    
    public void repeat() {
        Util.repeatMessage();
    }
    
    public void english() {
        setLocale(Locale.ENGLISH);
    }
    
    public void german() {
        setLocale(Locale.GERMAN);
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        FacesContext context = FacesContext.getCurrentInstance();
        context.getViewRoot().setLocale(locale);
    }

    public Locale getLocale() {
        return locale;
    }

    public String getLanguage() {
        return locale.getLanguage();
    }

    public String getcurrentLanguageTag() {
        return Util.getcurrentLanguageTag();
    }
}
