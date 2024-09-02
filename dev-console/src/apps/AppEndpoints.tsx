import { useRootSelector } from '@dslab/ra-root-selector';
import {
    Container,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
} from '@mui/material';
import { useState, useEffect } from 'react';
import {
    useDataProvider,
    RecordContextProvider,
    Labeled,
    TextField,
    useTranslate,
} from 'react-admin';
import { IdField } from '../components/IdField';

export const AppEndpointsView = () => {
    const dataProvider = useDataProvider();
    const translate = useTranslate();
    const { root: realmId } = useRootSelector();
    const [config, setConfig] = useState<any>();

    useEffect(() => {
        dataProvider
            .invoke({ path: 'realms/' + realmId + '/well-known/oauth2' })
            .then(data => {
                if (data) {
                    setConfig(data);
                }
            });
    }, [dataProvider]);

    if (!config) return null;

    const fields = [
        'issuer',
        'authorization_endpoint',
        'token_endpoint',
        'jwks_uri',
        'userinfo_endpoint',
        'introspection_endpoint',
        'revocation_endpoint',
        'registration_endpoint',
        'end_session_endpoint',
    ];

    return (
        <RecordContextProvider value={config}>
            <Container maxWidth="md" sx={{ mx: 0, px: 0 }}>
                <Table size="small">
                    <TableBody>
                        {fields.map(field => (
                            <TableRow
                                key={'endpoints.' + field}
                                sx={{
                                    '&:last-child td, &:last-child th': {
                                        border: 0,
                                    },
                                }}
                            >
                                <TableCell component="th" scope="row">
                                    <strong>
                                        {translate('field.' + field)}
                                    </strong>
                                </TableCell>
                                <TableCell align="right">
                                    <IdField source={field} />
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </Container>
        </RecordContextProvider>
    );
};
