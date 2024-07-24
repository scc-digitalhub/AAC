import { Menu, Button, Stack } from '@mui/material';
import { Box } from '@mui/system';
import { useState, MouseEvent, ReactElement } from 'react';
import {
    useTranslate,
    RaRecord,
    ShowButtonProps,
    useRecordContext,
    useResourceContext,
} from 'react-admin';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

export const DropDownButton = (props: DrodownButtonProps) => {
    const {
        icon,
        label = 'action.actions',
        record: recordProp,
        resource: resourceProp,
        children,
        ...rest
    } = props;
    const resource = useResourceContext(props);
    const record = useRecordContext(props);
    const translate = useTranslate();

    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const handleOpen = (event: MouseEvent<HTMLElement>): void => {
        event.stopPropagation();
        setAnchorEl(event.currentTarget);
    };
    const handleClose = (): void => {
        setAnchorEl(null);
    };

    return (
        <Box className="DropDownMenu" component="span">
            <Box>
                <Button
                    color="primary"
                    variant="contained"
                    aria-controls="simple-menu"
                    aria-label=""
                    aria-haspopup="true"
                    onClick={handleOpen}
                    startIcon={icon}
                    endIcon={<ExpandMoreIcon fontSize="small" />}
                >
                    {translate(label)}
                </Button>
            </Box>
            <Menu
                id="dropdown-menu"
                anchorEl={anchorEl}
                keepMounted
                open={Boolean(anchorEl)}
                onClose={handleClose}
            >
                <Stack direction={'column'}>{children}</Stack>
            </Menu>
        </Box>
    );
};

export type DrodownButtonProps<RecordType extends RaRecord = any> =
    ShowButtonProps & { children: ReactElement | ReactElement[] };
