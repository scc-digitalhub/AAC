import { useTranslate } from 'react-admin';
import { Box, Avatar, Container } from '@mui/material';
import AppShortcutIcon from '@mui/icons-material/AppShortcut';

import { PageTitle } from '../components/pageTitle';
import { ConnectionsList } from '../resources/connections';

export const ConnectionsPage = () => {
    const translate = useTranslate();
    return (
        <Container maxWidth="lg">
            <PageTitle
                text={translate('page.connections.header')}
                secondaryText={translate('page.connections.description')}
                icon={
                    <Avatar
                        sx={{
                            width: 72,
                            height: 72,
                            mb: 2,
                            alignItems: 'center',
                            display: 'inline-block',
                            textTransform: 'uppercase',
                            lineHeight: '102px',
                            backgroundColor: '#0066cc',
                        }}
                    >
                        <AppShortcutIcon sx={{ fontSize: 48 }} />
                    </Avatar>
                }
            />
            <ConnectionsList />
        </Container>
    );
};
