import * as React from 'react';
import { useState } from 'react';
import {
    useResourceContext,
    useGetResourceLabel,
    Button,
    useTranslate,
    Edit,
    EditButton,
    useEditContext,
    useDataProvider,
    useGetIdentity,
    SimpleList,
    FunctionToElement,
    RaRecord,
    DateField,
    Labeled,
    TextField,
    Toolbar,
    DeleteWithConfirmButton,
    LinearProgress,
    RecordContextProvider,
} from 'react-admin';
import { List, SimpleForm, TextInput } from 'react-admin';
import {
    Box,
    Grid,
    Typography,
    Card,
    ListItem,
    TableContainer,
    Paper,
    Table,
    TableBody,
    TableRow,
    TableCell,
    Avatar,
} from '@mui/material';
import Dialog from '@mui/material/Dialog';
import PersonIcon from '@mui/icons-material/Person';
import KeyIcon from '@mui/icons-material/Key';
import SwitchAccountIcon from '@mui/icons-material/SwitchAccount';
import EmailIcon from '@mui/icons-material/Email';
import AccountBoxIcon from '@mui/icons-material/AccountBox';
import PermContactCalendarIcon from '@mui/icons-material/PermContactCalendar';

import { List as MuiList } from '@mui/material';

import GridList from '../components/gridList';
import { PageTitle } from '../components/pageTitle';

const getIcon = (record: any) => {
    if (record && record.id === 'basicprofile') {
        return <AccountBoxIcon fontSize="large" />;
    }
    if (record && record.id === 'openid') {
        return <PermContactCalendarIcon fontSize="large" />;
    }
    if (record && record.id === 'email') {
        return <EmailIcon fontSize="large" />;
    }
    return <PersonIcon fontSize="large" />;
};

export const ProfilesList = () => {
    const translate = useTranslate();
    return (
        <Box component="div">
            <PageTitle
                text={translate('profiles_page.header')}
                secondaryText={translate('profiles_page.description')}
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
                        <PersonIcon sx={{ fontSize: 48 }} />
                    </Avatar>
                }
            />

            <List component="div" pagination={false} actions={false}>
                <GridList
                    // key={record => record.username}
                    cols={6}
                    primaryText={record => {
                        return record.id;
                    }}
                    icon={record => getIcon(record)}
                    secondaryText={record => (
                        <Box>
                            <TableContainer sx={{ width: 1 }}>
                                <Table>
                                    <TableBody>
                                        {Object.entries(record).map(
                                            (attr: any, key: any) => {
                                                return (
                                                    <TableRow key={attr[0]}>
                                                        <TableCell
                                                            component="th"
                                                            scope="row"
                                                            sx={{
                                                                fontSize:
                                                                    '0.9rem',
                                                            }}
                                                        >
                                                            <strong>
                                                                {translate(
                                                                    attr[0]
                                                                )}
                                                            </strong>
                                                        </TableCell>
                                                        <TableCell align="right">
                                                            {attr[1]}
                                                        </TableCell>
                                                    </TableRow>
                                                );
                                            }
                                        )}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </Box>
                    )}
                />
            </List>
        </Box>
    );
};
