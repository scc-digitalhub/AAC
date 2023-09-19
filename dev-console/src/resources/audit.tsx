import * as React from 'react';

import { FieldTitle, Labeled, useRecordContext } from 'react-admin';
import { Datagrid, DateField, List, TextField } from 'react-admin';
import { Box, Chip, Grid, Stack, Typography } from '@mui/material';
import LinearProgress from '@mui/material/LinearProgress';

export const AuditList = () => (
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
);

const AuditDetails = () => {
    const record = useRecordContext();
    if (!record || !record.data) {
        return <LinearProgress />;
    }
    if (record.type === 'USER_AUTHENTICATION_SUCCESS') {
        return <UserAuthAuditDetails />;
    }
    if (record.type === 'OAUTH2_TOKEN_GRANT') {
        return <OAuth2TokenAuditDetails />;
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

const OAuth2TokenAuditDetails = () => {
    const record = useRecordContext();
    if (!record || !record.data) {
        return <div></div>;
    }

    return (
        <Box sx={{ pb: 2 }}>
            <Stack direction="row" spacing={2}>
                <Labeled>
                    <TextField label="client" source="data.authorizedParty" />
                </Labeled>

                <Labeled>
                    <TextField label="audience" source="data.audience" />
                </Labeled>
            </Stack>
            <Stack direction="row" spacing={2} sx={{ mt: 2 }}>
                <Labeled>
                    <DateField
                        label="issuedAt"
                        source="data.issuedAt"
                        showTime
                    />
                </Labeled>

                <Labeled>
                    <DateField
                        label="expiresAt"
                        source="data.expiration"
                        showTime
                    />
                </Labeled>
                <Stack component="span">
                    <Typography color="textSecondary">
                        <FieldTitle label="scope" />
                    </Typography>
                    <Grid container spacing={1}>
                        {record.data.scope.map(function (scope: any) {
                            return (
                                <Grid item key={record.id + '.scope.' + scope}>
                                    <Chip label={scope} />
                                </Grid>
                            );
                        })}
                    </Grid>
                </Stack>
            </Stack>
        </Box>
    );
};
