import { useTranslate } from 'react-admin';
import { List } from 'react-admin';
import {
    Box,
    TableContainer,
    Table,
    TableBody,
    TableRow,
    TableCell,
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import EmailIcon from '@mui/icons-material/Email';
import AccountBoxIcon from '@mui/icons-material/AccountBox';
import PermContactCalendarIcon from '@mui/icons-material/PermContactCalendar';

import GridList from '../components/gridList';

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
                                                            fontSize: '0.9rem',
                                                        }}
                                                    >
                                                        <strong>
                                                            {translate(
                                                                'field.' +
                                                                    attr[0]
                                                            )}
                                                        </strong>
                                                    </TableCell>
                                                    <TableCell align="right">
                                                        <span> {attr[1]} </span>
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
    );
};
