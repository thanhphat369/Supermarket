package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import mypack.entity.Category;
import mypack.sessionbean.CategoryFacadeLocal;

@Named(value = "categoryMBean")
@ViewScoped
public class CategoryMBean implements Serializable {
    @EJB(beanInterface = CategoryFacadeLocal.class)
    private CategoryFacadeLocal categoryFacade;

    public List<Category> getLevel1Categories() {
        return categoryFacade.findLevel1();
    }

    public List<Category> getLevel2(Category c1) {
        return categoryFacade.findLevel2(c1);
    }

    public List<Category> getLevel3(Category c2) {
        return categoryFacade.findLevel3(c2);
    }
}
