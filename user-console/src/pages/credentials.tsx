import {
    useTranslate,
    LinearProgress,
    useGetList,
    useList,
    ResourceContextProvider,
    ListContextProvider,
} from 'react-admin';
import { Box, Typography, Avatar } from '@mui/material';
import VpnKeyIcon from '@mui/icons-material/VpnKey';

import { PageTitle } from '../components/pageTitle';
import { Spacer } from '../components/spacer';
import { WebAuthnGridList, WebAuthnAddToolbar } from '../resources/webauthn';
import { PasswordGridList, PasswordAddToolbar } from '../resources/password';

export const CredentialsPage = () => {
    const translate = useTranslate();

    return (
        <Box component="div">
            <PageTitle
                text={translate('page.credentials.header')}
                secondaryText={translate('page.credentials.description')}
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
                        <VpnKeyIcon sx={{ fontSize: 48 }} />
                    </Avatar>
                }
            />

            <Typography variant="h5" sx={{ mb: 2 }}>
                {translate('page.password.title')}
            </Typography>
            <PasswordList />
            <Spacer space="3rem" />
            <Typography variant="h5" sx={{ mb: 2 }}>
                {translate('page.webauthn.title')}
            </Typography>
            <WebAuthnList />
        </Box>
    );
};

// export interface CredentialsListProp<RecordType extends RaRecord = any> {
//     credentials?: RecordType[];
//     isLoading?: boolean;
// }

const PasswordList = () => {
    const translate = useTranslate();
    const { data, isLoading } = useGetList('password');
    const listContext = useList({ data, isLoading });
    if (isLoading) {
        return <LinearProgress />;
    }

    return (
        <ResourceContextProvider value="password">
            <ListContextProvider value={listContext}>
                <Typography variant="subtitle1" sx={{ mb: 2 }}>
                    {translate('page.password.subtitle')}
                </Typography>
                {data && data.length === 0 && <PasswordAddToolbar />}
                {data && data.length > 0 && <PasswordGridList />}
            </ListContextProvider>
        </ResourceContextProvider>
    );
};

const WebAuthnList = () => {
    const translate = useTranslate();
    const { data, isLoading } = useGetList('webauthn');
    const listContext = useList({ data, isLoading });
    if (isLoading) {
        return <LinearProgress />;
    }

    return (
        <ResourceContextProvider value="webauthn">
            <ListContextProvider value={listContext}>
                <Typography variant="subtitle1" sx={{ mb: 2 }}>
                    {translate('page.webauthn.subtitle')}
                </Typography>
                <WebAuthnAddToolbar />
                <Spacer />
                {data && data.length > 0 && <WebAuthnGridList />}
            </ListContextProvider>
        </ResourceContextProvider>
    );
};
