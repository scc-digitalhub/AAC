import { ResourceContextProvider, useTranslate } from 'react-admin';
import { Container, Grid, Typography, Avatar } from '@mui/material';

import { PageTitle } from '../components/pageTitle';
import LockIcon from '@mui/icons-material/Lock';
import { AuditList } from '../resources/audit';

export const SecurityPage = () => {
    const translate = useTranslate();

    return (
        <Container maxWidth={false}>
            <PageTitle
                text={translate('page.security.header')}
                secondaryText={translate('page.security.description')}
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
                        <LockIcon sx={{ fontSize: 48 }} />
                    </Avatar>
                }
            />

            <Grid container spacing={2}>
                <Grid item xs={12} zeroMinWidth>
                    <Typography variant="h5" sx={{ mb: 2 }}>
                        {translate('page.audit.title')}
                    </Typography>
                    <Typography variant="subtitle1" sx={{ mb: 2 }}>
                        {translate('page.audit.description')}
                    </Typography>
                    <AuditPage />
                </Grid>
            </Grid>
        </Container>
    );
};

const AuditPage = () => {
    return (
        <ResourceContextProvider value="audit">
            <AuditList />
        </ResourceContextProvider>
    );
};
