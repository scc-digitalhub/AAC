import { MouseEventHandler, ReactElement, useState } from 'react';
import {
    BooleanInput,
    Button,
    ButtonProps,
    required,
    SaveButton,
    SimpleForm,
    Toolbar,
    useGetResourceLabel,
    useDataProvider,
    useNotify,
    useRefresh,
    useResourceContext,
    useTranslate,
} from 'react-admin';
import {
    Box,
    Breakpoint,
    Dialog,
    DialogContent,
    DialogTitle,
    IconButton,
    styled,
    Typography,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import UploadIcon from '@mui/icons-material/Upload';
import { AceEditorInput } from '@dslab/ra-ace-editor';
import { toYaml } from '@dslab/ra-export-record-button';
import yaml from 'yaml';
import { useRootSelector } from '@dslab/ra-root-selector';
import { get } from 'lodash';

const defaultIcon = <UploadIcon />;

export const ImportButton = (props: ImportButtonProps) => {
    const {
        title = 'page.importer.title',
        icon = defaultIcon,
        maxWidth = 'md',
        fullWidth = true,
        label = 'action.import_one',
        color = 'info',
        variant,
        onSuccess,
        record,
        sx,
    } = props;

    const resource = useResourceContext(props);
    const { root: realm } = useRootSelector();

    const [open, setOpen] = useState(false);
    const notify = useNotify();
    const refresh = useRefresh();
    const dataProvider = useDataProvider();

    const closeDialog = () => {
        setOpen(false);
    };

    // PUT /{resource}/{realm}?reset= with the yaml as-is:
    // the server parses it, single element or list, and handles reset and realm
    const handleSubmit = (data: any) => {
        const body = new FormData();
        body.append('yaml', toYaml(data.value));

        return dataProvider
            .invoke({
                path: resource + '/' + realm,
                params: { reset: !!data.reset },
                body: body as any,
                options: { method: 'PUT' },
            })
            .then(json => {
                closeDialog();
                refresh();

                if (onSuccess) {
                    return onSuccess(json);
                }

                notify('ra.notification.created', {
                    type: 'info',
                    messageArgs: { smart_count: 1 },
                });
            })
            .catch(error => {
                notify(
                    typeof error === 'string'
                        ? error
                        : error?.message || 'ra.notification.http_error',
                    { type: 'error' }
                );
            });
    };

    const handleDialogOpen: MouseEventHandler<HTMLButtonElement> = e => {
        setOpen(true);
        e.stopPropagation();
    };

    const handleDialogClose: MouseEventHandler<HTMLButtonElement> = e => {
        closeDialog();
        e.stopPropagation();
    };

    return (
        <>
            <Button
                label={label}
                color={color}
                onClick={handleDialogOpen}
                className={ImportButtonClasses.button}
                variant={variant}
            >
                {icon}
            </Button>

            <ImportDialog
                maxWidth={maxWidth}
                fullWidth={fullWidth}
                onClose={handleDialogClose}
                aria-labelledby="import-dialog-title"
                open={open}
                className={ImportButtonClasses.dialog}
                scroll="paper"
                sx={sx}
            >
                <ImportContent
                    title={title}
                    record={{
                        reset: false,
                        value: record || {
                            realm: realm,
                            name: '',
                        },
                    }}
                    handleClose={handleDialogClose}
                    handleSubmit={handleSubmit}
                />
            </ImportDialog>
        </>
    );
};

const ImportContent = (props: {
    title: string | ReactElement;
    record: any;
    handleClose: MouseEventHandler;
    handleSubmit: (data: any) => void;
}) => {
    const { title, record, handleClose, handleSubmit } = props;
    const translate = useTranslate();
    const resource = useResourceContext();
    const getResourceLabel = useGetResourceLabel();
    const resourceLabel = getResourceLabel(resource, 1);

    return (
        <>
            <div className={ImportButtonClasses.header}>
                <DialogTitle
                    id="create-dialog-title"
                    className={ImportButtonClasses.title}
                >
                    {typeof title === 'string'
                        ? translate(title, {
                              _: title,
                              resource: resourceLabel,
                          })
                        : title}
                </DialogTitle>

                <IconButton
                    className={ImportButtonClasses.closeButton}
                    aria-label={translate('ra.action.close')}
                    title={translate('ra.action.close')}
                    onClick={handleClose}
                    size="small"
                >
                    <CloseIcon fontSize="small" />
                </IconButton>
            </div>

            <DialogContent sx={{ p: 0 }}>
                <Box p={2}>
                    <Typography variant="body2">
                        {translate('page.importer.helperText')}
                    </Typography>
                </Box>
                <SimpleForm
                    record={record}
                    onSubmit={handleSubmit}
                    toolbar={
                        <Toolbar>
                            <SaveButton
                                label="action.import_one"
                                alwaysEnable
                            />
                        </Toolbar>
                    }
                >
                    <AceEditorInput
                        mode="yaml"
                        theme="github"
                        source="value"
                        label="fields.value.title"
                        parse={toYaml}
                        format={yaml.parse}
                        validate={[required()]}
                        minLines={20}
                        width="40vw"
                    />
                    <BooleanInput
                        source="reset"
                        label="page.importer.resetOnImport.label"
                        helperText="page.importer.resetOnImport.helperText"
                    />
                </SimpleForm>
            </DialogContent>
        </>
    );
};

export type ImportButtonProps = {
    title?: string | ReactElement;
    resource?: string;
    record?: any;
    onSuccess?: (data: any) => void;
    maxWidth?: Breakpoint | false;
    fullWidth?: boolean;
    label?: string;
    variant?: 'text' | 'outlined' | 'contained';
    icon?: ReactElement;
    sx?: any;
} & Pick<ButtonProps, 'color'>;

const PREFIX = 'RaImportButton';

export const ImportButtonClasses = {
    button: `${PREFIX}-button`,
    dialog: `${PREFIX}-dialog`,
    header: `${PREFIX}-header`,
    title: `${PREFIX}-title`,
    closeButton: `${PREFIX}-close-button`,
};

const ImportDialog = styled(Dialog, {
    name: PREFIX,
    overridesResolver: (_props, styles) => styles.root,
})(({ theme }) => ({
    [`& .${ImportButtonClasses.title}`]: {
        padding: theme.spacing(0),
    },
    [`& .${ImportButtonClasses.header}`]: {
        padding: theme.spacing(2, 2),
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
    },
    [`& .${ImportButtonClasses.closeButton}`]: {
        height: 'fit-content',
    },
}));
