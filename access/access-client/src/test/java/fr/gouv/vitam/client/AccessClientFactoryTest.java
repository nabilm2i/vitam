/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.access.client.AccessClient;
import fr.gouv.vitam.access.client.AccessClientFactory;
import fr.gouv.vitam.access.client.AccessClientFactory.AccessClientType;
import fr.gouv.vitam.access.client.AccessOperationsClientMock;
import fr.gouv.vitam.access.client.AccessOperationsClientRest;

/**
 * Test class for client (and parameters) factory
 */
public class AccessClientFactoryTest {

    @Before
    public void initFileConfiguration() {
        AccessClientFactory.getInstance().changeConfigurationFile("access-client.conf");
    }

    @Test
    public void getClientInstanceTest() {
        try {
            AccessClientFactory.setConfiguration(AccessClientType.OPERATIONS, null, 10);
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {

        }
        try {
            AccessClientFactory.setConfiguration(AccessClientType.OPERATIONS, "localhost", -10);
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {

        }
        try {
            AccessClientFactory.setConfiguration(null, null, 10);
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {

        }
        AccessClientFactory.setConfiguration(AccessClientType.MOCK_OPERATIONS, null, -1);

        final AccessClient client =
            AccessClientFactory.getInstance().getAccessOperationClient();
        assertNotNull(client);

        final AccessClient client2 =
            AccessClientFactory.getInstance().getAccessOperationClient();
        assertNotNull(client2);

        assertNotSame(client, client2);
    }

    @Test
    public void changeDefaultClientTypeTest() {
        final AccessClient client =
            AccessClientFactory.getInstance().getAccessOperationClient();
        assertTrue(client instanceof AccessOperationsClientRest);
        final AccessClientFactory.AccessClientType type = AccessClientFactory.getDefaultAccessClientType();
        assertNotNull(type);
        assertEquals(AccessClientType.OPERATIONS, type);

        AccessClientFactory.setConfiguration(AccessClientType.MOCK_OPERATIONS, null, 0);
        final AccessClient client2 = AccessClientFactory.getInstance().getAccessOperationClient();
        // a voir- bug
        assertTrue(client2 instanceof AccessOperationsClientMock);
        final AccessClientFactory.AccessClientType type2 = AccessClientFactory.getDefaultAccessClientType();
        assertNotNull(type2);
        assertEquals(AccessClientType.MOCK_OPERATIONS, type2);

        AccessClientFactory.setConfiguration(AccessClientFactory.AccessClientType.OPERATIONS, "server", 1025);
        final AccessClient client3 = AccessClientFactory.getInstance().getAccessOperationClient();
        assertTrue(client3 instanceof AccessOperationsClientRest);
        final AccessClientFactory.AccessClientType type3 = AccessClientFactory.getDefaultAccessClientType();
        assertNotNull(type3);
        assertEquals(AccessClientType.OPERATIONS, type3);
    }

    @Test
    public void testInitWithConfigurationFile() {
        final AccessClient client =
            AccessClientFactory.getInstance().getAccessOperationClient();
        assertTrue(client instanceof AccessOperationsClientRest);
        final AccessClientFactory.AccessClientType type = AccessClientFactory.getDefaultAccessClientType();
        assertNotNull(type);
        assertEquals(AccessClientType.OPERATIONS, type);
    }

}
