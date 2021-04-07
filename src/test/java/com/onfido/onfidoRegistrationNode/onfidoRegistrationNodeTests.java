package com.onfido.onfidoRegistrationNode;

import com.onfido.exceptions.OnfidoException;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.support.hierarchical.Node;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class onfidoRegistrationNodeTests {

    @Test
    void test_initialization_of_onfidoRegistrationNode() throws NodeProcessException {
        CoreWrapper coreWrapper = spy(CoreWrapper.class);
        onfidoRegistrationNode.Config config = spy(onfidoRegistrationNode.Config.class);
        when(config.onfidoToken()).thenReturn("token".toCharArray());

         new onfidoRegistrationNode(config, coreWrapper);

        onfidoRegistrationNode node = mock(onfidoRegistrationNode.class);

    }

}
