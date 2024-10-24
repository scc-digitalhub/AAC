import { Link } from 'react-router-dom';
import { useResourceContext, useCreatePath, useRecordContext } from 'ra-core';
import { Button, ShowButtonProps } from 'react-admin';
import VisibilityIcon from '@mui/icons-material/Visibility';

const defaultIcon = <VisibilityIcon />;
const DEV_URL: string = process.env.REACT_APP_DEVELOPER_CONSOLE as string;

export const SelectButton = (props: ShowButtonProps) => {
    const {
        className,
        icon = defaultIcon,
        label = 'ra.action.show',
        resource: resourceProp,
        variant,
        ...rest
    } = props;

    const resource = useResourceContext(props);
    const record = useRecordContext(props);

    const path = DEV_URL + '/-/' + record.id;

    return (
        <Button
            component={Link}
            to={path}
            className={className}
            label={label}
            variant={variant}
            {...(rest as any)}
        >
            {icon}
        </Button>
    );
};

export default SelectButton;
