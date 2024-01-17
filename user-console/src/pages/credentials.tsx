import {
    useTranslate,
    LinearProgress,
    useGetList,
    useList,
    ResourceContextProvider,
    ListContextProvider,
    useGetIdentity,
} from 'react-admin';
import { Box, Typography, Avatar, Alert, Container } from '@mui/material';
import VpnKeyIcon from '@mui/icons-material/VpnKey';

import { PageTitle } from '../components/pageTitle';
import { Spacer } from '../components/spacer';
import { WebAuthnGridList, WebAuthnAddToolbar } from '../resources/webauthn';
import { PasswordGridList, PasswordAddToolbar } from '../resources/password';
import { useState, useEffect } from 'react';

export const CredentialsPage = () => {
    const translate = useTranslate();
    const { data, isLoading } = useGetIdentity();
    //load accounts and credential to check if we should suggest actions
    //TODO refactor
    const { data: accounts, isLoading: isLoadingAccounts } = useGetList(
        'accounts',
        {
            pagination: { page: 1, perPage: 100 },
        }
    );
    const { data: passwords, isLoading: isLoadingPasswords } = useGetList(
        'password',
        {
            pagination: { page: 1, perPage: 100 },
        }
    );
    const { data: keys, isLoading: isLoadingKeys } = useGetList('webauthn', {
        pagination: { page: 1, perPage: 100 },
    });

    const [shouldCreateCredentials, setShouldCreateCredentials] =
        useState<boolean>(false);

    useEffect(() => {
        if (!isLoadingAccounts && !isLoadingPasswords && !isLoadingKeys) {
            const account = accounts?.find(a => a.authority === 'internal');
            if (account && passwords?.length === 0 && keys?.length === 0) {
                setShouldCreateCredentials(true);
            }
        }
    }, [
        accounts,
        passwords,
        keys,
        isLoadingAccounts,
        isLoadingPasswords,
        isLoadingKeys,
    ]);
    return (
        <Container maxWidth="lg">
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
            {shouldCreateCredentials && (
                <Alert
                    severity="warning"
                    variant="filled"
                    sx={{ width: '100%', mb: 2 }}
                >
                    {translate('alert.missing_credentials')}
                </Alert>
            )}

            <Typography variant="h5" sx={{ mb: 2 }}>
                {translate('page.password.title')}
            </Typography>
            <PasswordList />
            <Spacer space="3rem" />
            <Typography variant="h5" sx={{ mb: 2 }}>
                {translate('page.webauthn.title')}
            </Typography>
            <WebAuthnList />
        </Container>
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
