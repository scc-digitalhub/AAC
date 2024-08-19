import {
    Box,
    Breakpoint,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    FormControl,
    FormGroup,
    FormLabel,
    Grid,
    IconButton,
    Stack,
    styled,
    Switch,
    Typography,
} from '@mui/material';
import {
    Fragment,
    ReactElement,
    useCallback,
    useEffect,
    useState,
} from 'react';
import {
    RaRecord,
    ButtonProps,
    useRecordContext,
    useResourceContext,
    useTranslate,
    Button,
    LoadingIndicator,
    useDataProvider,
    useNotify,
    ReferenceManyField,
    ReferenceManyFieldProps,
} from 'react-admin';
import { useRootSelector } from '@dslab/ra-root-selector';

import LocalPoliceIcon from '@mui/icons-material/LocalPolice';
import CloseIcon from '@mui/icons-material/Close';
import CancelIcon from '@mui/icons-material/ErrorOutline';
import ConfirmIcon from '@mui/icons-material/CheckCircle';

export const ReferenceDetachedField = (props: ReferenceManyFieldProps) => {
    const { record, resource, source, reference } = props;
    const translate = useTranslate();
    const dataProvider = useDataProvider();
    const notify = useNotify();
    const { root: realmId } = useRootSelector();
    const [authorities, setAuthorities] = useState<any[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    //hard-coded
    const roles = ['ROLE_DEVELOPER', 'ROLE_ADMIN'];

    useEffect(() => {
        if (record) {
            dataProvider
                .invoke({
                    path:
                        resource +
                        '/' +
                        realmId +
                        '/' +
                        record.id +
                        '/authorities',
                })
                .then(data => {
                    if (data) {
                        setAuthorities(data);
                    }
                });
        }
        return () => {
            setIsLoading(false);
        };
    }, [dataProvider, record]);

    const handleSwitch = role => {
        return (event: React.ChangeEvent<HTMLInputElement>) => {
            setAuthorities(list => {
                const keep = list.filter(e => e.role !== role);
                const edit = event.target.checked
                    ? [{ realm: realmId, role: role }]
                    : [];
                return [...keep, ...edit];
            });
        };
    };

    const handleSave = e => {
        dataProvider
            .invoke({
                path:
                    resource + '/' + realmId + '/' + record.id + '/authorities',
                body: JSON.stringify(authorities),
                options: {
                    method: 'PUT',
                },
            })
            .then(() => {
                notify('notification.updated');
                if (onSuccess) {
                    onSuccess();
                }
            });

        handleClose(e);
    };

    if (!record || !resource) return null;
    if (isLoading) return <LoadingIndicator />;

    return (
        <>
            <DialogContent>
                <FormControl component="fieldset" fullWidth>
                    <FormLabel component="legend" sx={{ mb: 2 }}>
                        {translate('dialog.authorities.helperText')}
                    </FormLabel>
                    <FormGroup>
                        {roles.map(r => {
                            const checked =
                                authorities.find(a => a.role === r) || false;

                            return (
                                <Box key={r} mb={2}>
                                    <Grid container mb={1}>
                                        <Grid item xs={10}>
                                            <Stack rowGap={1}>
                                                <Typography variant="h6">
                                                    {translate(
                                                        'authorities.' +
                                                            r +
                                                            '.name'
                                                    )}
                                                </Typography>
                                                <Typography
                                                    variant="body2"
                                                    fontFamily={'monospace'}
                                                >
                                                    {r}
                                                </Typography>
                                                <Typography variant="body2">
                                                    {translate(
                                                        'authorities.' +
                                                            r +
                                                            '.description'
                                                    )}
                                                </Typography>
                                            </Stack>
                                        </Grid>
                                        <Grid item xs={2}>
                                            <Switch
                                                checked={checked}
                                                onChange={handleSwitch(r)}
                                                name={r}
                                            />
                                        </Grid>
                                    </Grid>
                                    <Divider />
                                </Box>
                            );
                        })}
                    </FormGroup>
                </FormControl>
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={isLoading}
                    onClick={handleClose}
                    startIcon={<CancelIcon />}
                    label={translate('ra.action.cancel')}
                />
                <Button
                    disabled={isLoading}
                    onClick={handleSave}
                    autoFocus
                    startIcon={<ConfirmIcon />}
                    label={translate('ra.action.save')}
                />
            </DialogActions>
        </>
    );
};
