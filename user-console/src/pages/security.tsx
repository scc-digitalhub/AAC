import * as React from 'react';
import { Link } from 'react-router-dom';
import {
    Labeled,
    ListBase,
    ResourceContextProvider,
    useGetIdentity,
    useGetList,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { Datagrid, DateField, List, NumberField, TextField } from 'react-admin';
import { Box, List as MuiList, ListItem, Stack } from '@mui/material';
import LinearProgress from '@mui/material/LinearProgress';
import { Card, CardContent, CardActions, CardHeader } from '@mui/material';
import { Container, Grid, Typography, Button, Avatar } from '@mui/material';

import GroupIcon from '@mui/icons-material/Group';
import KeyIcon from '@mui/icons-material/Key';
import AppShortcutIcon from '@mui/icons-material/AppShortcut';
import { PageTitle } from '../components/pageTitle';
import { transform } from 'typescript';
import LockIcon from '@mui/icons-material/Lock';

export const SecurityPage = () => {
    const { data, isLoading } = useGetIdentity();
    const translate = useTranslate();
    if (isLoading === true || !data) {
        return <LinearProgress />;
    }
    return (
        <Container maxWidth={false}>
            <PageTitle
                text={translate('security_page.header')}
                secondaryText={translate('security_page.description')}
                icon={
                    data.username && (
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
                    )
                }
            />

            <Grid container spacing={2}>
                <Grid item xs={12} zeroMinWidth>
                    <Typography variant="h5" sx={{ mb: 2 }}>
                        {translate('security_page.audit.title')}
                    </Typography>
                    <Typography variant="subtitle1" sx={{ mb: 2 }}>
                        {translate('security_page.audit.description')}
                    </Typography>
                    <AuditList />
                </Grid>
            </Grid>
        </Container>
    );
};

const AuditList = () => {
    return (
        <ResourceContextProvider value="audit">
            <List actions={false}>
                <Datagrid
                    rowClick="expand"
                    expand={<AuditDetails />}
                    bulkActionButtons={false}
                >
                    <DateField source="time" showTime />
                    <TextField source="type" />
                </Datagrid>
            </List>
        </ResourceContextProvider>
    );
};

const AuditDetails = () => {
    const record = useRecordContext();
    if (!record || !record.data) {
        return <LinearProgress />;
    }
    if (record.type == 'USER_AUTHENTICATION_SUCCESS') {
        return <UserAuthAuditDetails />;
    }

    return <div></div>;
};

const UserAuthAuditDetails = () => {
    const record = useRecordContext();
    if (!record || !record.data) {
        return <div></div>;
    }

    return (
        <Box sx={{ pb: 2 }}>
            <Stack direction="row" spacing={2}>
                <Labeled>
                    <TextField label="authority" source="data.authority" />
                </Labeled>

                <Labeled>
                    <TextField label="provider" source="data.provider" />
                </Labeled>
            </Stack>

            {record.data.details && (
                <Stack direction="row" spacing={2} sx={{ mt: 2 }}>
                    {record.data.details.remoteAddress && (
                        <Labeled>
                            <TextField
                                label="remoteAddress"
                                source="data.details.remoteAddress"
                            />
                        </Labeled>
                    )}
                    {record.data.details.protocol && (
                        <Labeled>
                            <TextField
                                label="protocol"
                                source="data.details.protocol"
                            />
                        </Labeled>
                    )}
                    {record.data.details.userAgent && (
                        <Labeled>
                            <TextField
                                label="userAgent"
                                source="data.details.userAgent"
                            />
                        </Labeled>
                    )}
                </Stack>
            )}
        </Box>
    );
};
