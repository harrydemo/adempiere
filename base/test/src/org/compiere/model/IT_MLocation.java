/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2020 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
package org.compiere.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.adempiere.test.CommonGWSetup;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Model")
@Tag("MLocation")
class IT_MLocation extends CommonGWSetup {

    // Test: Specific variables
    private MLocation location = null;

    @Test
    void testMLocationCreation() {

        location = new MLocation(ctx, 0, trxName);
        // location.loadDefaults();
        location.setC_Country_ID(100);
        location.setC_Region_ID(103);
        location.setCity("Windsor");
        location.setAddress1("nyb");
        location.setAddress2("");
        location.setPostal("95492");
        location.setPostal_Add("95492");
        location.setAD_Org_ID(0);

        assertTrue(location.save(), "MLocation.save()");

    }

}
